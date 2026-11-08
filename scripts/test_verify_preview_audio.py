"""Offline tests for deterministic Projectivy output-audio assertions."""
import importlib.util
import json
import subprocess
import sys
import tempfile
import unittest
import wave
from pathlib import Path

import numpy as np

SPEC = importlib.util.spec_from_file_location(
    "verify_preview_audio",
    Path(__file__).with_name("verify-preview-audio.py"),
)
VERIFIER = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(VERIFIER)


class PreviewAudioAnalysisTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.directory = Path(self.temporary.name)

    def tearDown(self):
        self.temporary.cleanup()

    def write_wav(self, name: str, frequency: int | None, seconds: int = 5) -> Path:
        path = self.directory / name
        rate = 48_000
        time = np.arange(rate * seconds, dtype=np.float64) / rate
        samples = np.zeros(len(time), dtype=np.int16)
        if frequency is not None:
            samples = np.rint(12_000 * np.sin(2 * np.pi * frequency * time)).astype(np.int16)
        with wave.open(str(path), "wb") as output:
            output.setnchannels(1)
            output.setsampwidth(2)
            output.setframerate(rate)
            output.writeframes(samples.astype("<i2").tobytes())
        return path

    def test_440_hz_fixture_passes_a_assertion(self):
        result = VERIFIER.analyze_tone(self.write_wav("a.wav", 440), 440)

        self.assertEqual("PASS", result["status"])
        self.assertEqual(440.0, result["medianPeakHz"])
        self.assertEqual(result["windows"], result["matchingWindows"])

    def test_880_hz_fixture_passes_b_assertion(self):
        result = VERIFIER.analyze_tone(self.write_wav("b.wav", 880), 880)

        self.assertEqual("PASS", result["status"])
        self.assertEqual(880.0, result["medianPeakHz"])
        self.assertEqual(result["windows"], result["matchingWindows"])

    def test_wrong_tone_fails_expected_fixture_assertion(self):
        result = VERIFIER.analyze_tone(self.write_wav("wrong.wav", 880), 440)

        self.assertEqual("FAIL_PRODUCT", result["status"])
        self.assertEqual(880.0, result["medianPeakHz"])

    def test_short_capture_does_not_pass_without_three_windows(self):
        result = VERIFIER.analyze_tone(self.write_wav("short.wav", 440, seconds=2), 440)

        self.assertEqual("INCONCLUSIVE", result["status"])
        self.assertLess(result["windows"], 3)

    def test_silence_passes_loss_assertion_and_fails_tone_assertion(self):
        silence = self.write_wav("silence.wav", None)

        self.assertEqual("PASS", VERIFIER.analyze_silence(silence)["status"])
        self.assertEqual("FAIL_PRODUCT", VERIFIER.analyze_tone(silence, 440)["status"])

    def test_too_short_silence_capture_is_inconclusive(self):
        self.assertEqual(
            "INCONCLUSIVE",
            VERIFIER.analyze_silence(self.write_wav("short-silence.wav", None, seconds=2))["status"],
        )

    def test_cli_propagates_insufficient_capture_as_inconclusive(self):
        audio_a = self.write_wav("short-a.wav", 440, seconds=2)
        audio_b = self.write_wav("short-b.wav", 880, seconds=2)
        focus_loss = self.write_wav("short-loss.wav", None, seconds=2)
        output = self.directory / "result.json"
        script = Path(__file__).with_name("verify-preview-audio.py")

        result = subprocess.run(
            [sys.executable, str(script), "--a", str(audio_a), "--b", str(audio_b),
             "--focus-loss", str(focus_loss), "--output", str(output)],
            capture_output=True, text=True, check=False,
        )

        self.assertEqual(1, result.returncode)
        self.assertEqual("INCONCLUSIVE", json.loads(output.read_text())["outcome"])


if __name__ == "__main__":
    unittest.main()
