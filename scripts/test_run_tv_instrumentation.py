import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

from run_tv_instrumentation import classify_instrumentation, parser
from tv_adb import is_allowed_target


SCRIPT = Path(__file__).with_name("run_tv_instrumentation.py")
TVCTL = Path(__file__).with_name("tvctl")


class TvInstrumentationRunnerTests(unittest.TestCase):
    def test_physical_device_requires_explicit_opt_in(self):
        serial = "192.168.3.2:5555"
        self.assertFalse(is_allowed_target(serial))
        args = parser().parse_args([
            "--serial", serial,
            "--adb", "/tmp/fake-adb",
            "--allow-physical",
        ])
        self.assertTrue(is_allowed_target(args.serial, allow_physical=args.allow_physical))

    def test_accepts_provider_mime_instrumentation_class(self):
        args = parser().parse_args([
            "--serial", "emulator-5554",
            "--adb", "/tmp/fake-adb",
            "--class", "com.auramusic.app.tv.TvPreviewContentProviderInstrumentedTest",
        ])
        self.assertEqual(
            "com.auramusic.app.tv.TvPreviewContentProviderInstrumentedTest",
            args.test_class,
        )

    def test_classifies_pass_product_failure_infrastructure_failure_and_zero_tests(self):
        self.assertEqual(("PASS", 2), classify_instrumentation(0, "OK (2 tests)"))
        self.assertEqual(("FAIL_PRODUCT", None), classify_instrumentation(0, "FAILURES!!!"))
        self.assertEqual(("FAIL_TEST_INFRA", None), classify_instrumentation(1, "INSTRUMENTATION_FAILED: runner crashed"))
        self.assertEqual(("INCONCLUSIVE", 0), classify_instrumentation(0, "OK (0 tests)"))
        self.assertEqual(("FAIL_TEST_INFRA", None), classify_instrumentation(0, "no instrumentation summary"))

    def test_runs_only_against_explicit_emulator_and_writes_machine_evidence(self):
        with tempfile.TemporaryDirectory() as temporary:
            temp = Path(temporary)
            fake_adb = temp / "fake-adb"
            call_log = temp / "adb-calls.jsonl"
            app_apk = temp / "app.apk"
            test_apk = temp / "test.apk"
            output = temp / "evidence"
            app_apk.write_bytes(b"app apk fixture")
            test_apk.write_bytes(b"test apk fixture")
            fake_adb.write_text(
                "#!/usr/bin/env python3\n"
                "import json, os, sys\n"
                "args=sys.argv[1:]\n"
                "open(os.environ['FAKE_ADB_LOG'],'a').write(json.dumps(args)+'\\n')\n"
                "sub=args[4:]\n"
                "if sub == ['get-state']: print('device')\n"
                "elif sub[:3] == ['shell','getprop','ro.product.model']: print('Mock TV')\n"
                "elif sub[:3] == ['shell','getprop','ro.build.version.sdk']: print('36')\n"
                "elif sub[:3] == ['shell','getprop','ro.build.fingerprint']: print('mock/tv/fingerprint')\n"
                "elif sub[:3] == ['shell','getprop','sys.boot_completed']: print('1')\n"
                "elif sub[:3] == ['shell','pm','list']: print('feature:android.hardware.type.television\\nfeature:android.software.leanback')\n"
                "elif sub[:3] == ['shell','dumpsys','package']: print('versionName=4.73 versionCode=98')\n"
                "elif sub[:2] == ['install','-r']: print('Success')\n"
                "elif sub[:3] == ['shell','am','instrument']: print('INSTRUMENTATION_STATUS: numtests=2\\nOK (2 tests)\\nINSTRUMENTATION_CODE: -1')\n"
                "elif sub[0:2] == ['logcat','-d']: print('mock logcat')\n"
                "elif sub[:3] == ['shell','dumpsys','media.audio_flinger']: print('mock audio')\n"
                "elif sub[:3] == ['shell','dumpsys','activity']: print('mock foreground')\n"
                "else: print('unexpected mock ADB args: '+repr(sub), file=sys.stderr); sys.exit(2)\n"
            )
            fake_adb.chmod(0o755)
            environment = dict(
                os.environ,
                FAKE_ADB_LOG=str(call_log),
                TVCTL_ADB=str(fake_adb),
                TVCTL_ADB_PORT="5038",
                TVCTL_SERIAL="emulator-5554",
                TVCTL_OUT=str(output),
                TVCTL_APP_APK=str(app_apk),
                TVCTL_TEST_APK=str(test_apk),
            )
            completed = subprocess.run(
                ["bash", str(TVCTL), "test", "standalone"],
                capture_output=True,
                text=True,
                env=environment,
                check=False,
            )

            self.assertEqual(0, completed.returncode, completed.stderr + completed.stdout)
            evidence_dir = next(output.glob("tv_instrumentation_*"))
            result = json.loads((evidence_dir / "result.json").read_text())
            self.assertEqual("PASS", result["status"])
            self.assertEqual(2, result["test_count"])
            self.assertEqual("Mock TV", result["device"]["model"])
            self.assertEqual("4.73", result["device"]["projectivy_version"])
            self.assertTrue((evidence_dir / "instrumentation.txt").is_file())
            self.assertTrue((evidence_dir / "logcat.txt").is_file())
            calls = [json.loads(line) for line in call_log.read_text().splitlines()]
            self.assertTrue(calls)
            self.assertTrue(all(call[:4] == ["-P", "5038", "-s", "emulator-5554"] for call in calls))
            self.assertEqual(2, sum(call[4:6] == ["install", "-r"] for call in calls))
            self.assertTrue(any(call[4:8] == ["shell", "am", "instrument", "-w"] for call in calls))

    def test_rejects_physical_serial_before_adb_or_evidence_creation(self):
        with tempfile.TemporaryDirectory() as temporary:
            temp = Path(temporary)
            fake_adb = temp / "fake-adb"
            call_log = temp / "adb-calls.jsonl"
            app_apk = temp / "app.apk"
            test_apk = temp / "test.apk"
            output = temp / "evidence"
            app_apk.write_bytes(b"app")
            test_apk.write_bytes(b"test")
            fake_adb.write_text("#!/bin/sh\nprintf called >> \"$FAKE_ADB_LOG\"\n")
            fake_adb.chmod(0o755)
            environment = dict(
                os.environ,
                FAKE_ADB_LOG=str(call_log),
                TVCTL_ADB=str(fake_adb),
                TVCTL_ADB_PORT="5038",
                TVCTL_SERIAL="192.168.3.2:5555",
                TVCTL_OUT=str(output),
                TVCTL_APP_APK=str(app_apk),
                TVCTL_TEST_APK=str(test_apk),
            )
            completed = subprocess.run(
                ["bash", str(TVCTL), "test", "standalone"],
                capture_output=True,
                text=True,
                env=environment,
                check=False,
            )

            self.assertEqual(2, completed.returncode)
            self.assertIn("non-allowlisted device serial", completed.stderr)
            self.assertFalse(call_log.exists())
            self.assertFalse(output.exists())


if __name__ == "__main__":
    unittest.main()
