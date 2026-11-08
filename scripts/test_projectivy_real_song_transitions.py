"""Unit tests for focused-card hierarchy parsing used by the live transition verifier."""
import importlib.util
import unittest
from pathlib import Path

SPEC = importlib.util.spec_from_file_location(
    "verify_projectivy_real_song_transitions",
    Path(__file__).with_name("verify-projectivy-real-song-transitions.py"),
)
VERIFIER = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(VERIFIER)


class FocusedProgramInfoTest(unittest.TestCase):
    def test_emulator_serials_are_allowed_by_default(self):
        self.assertTrue(VERIFIER.is_allowed_target("emulator-5554", allow_physical=False))
        self.assertTrue(VERIFIER.is_allowed_target("emulator-5556", allow_physical=False))

    def test_physical_serial_requires_explicit_opt_in(self):
        self.assertFalse(VERIFIER.is_allowed_target("192.168.3.2:5555", allow_physical=False))
        self.assertTrue(VERIFIER.is_allowed_target("192.168.3.2:5555", allow_physical=True))

    def test_returns_channel_ancestor_for_focused_card(self):
        hierarchy = """<hierarchy><node content-desc="AuraMusic"><node focused="true" content-desc="Song A" bounds="[1,2][3,4]"/></node></hierarchy>"""
        info = VERIFIER.focused_program_info(hierarchy, "Song A")
        self.assertIsNotNone(info)
        self.assertEqual(["AuraMusic"], info["ancestorContentDescriptions"])
        self.assertEqual("[1,2][3,4]", info["attributes"]["bounds"])

    def test_ignores_same_title_without_focus(self):
        hierarchy = """<hierarchy><node content-desc="AuraMusic"><node focused="false" content-desc="Song A"/></node></hierarchy>"""
        self.assertIsNone(VERIFIER.focused_program_info(hierarchy, "Song A"))

    def test_does_not_match_focused_different_title(self):
        hierarchy = """<hierarchy><node content-desc="AuraMusic"><node focused="true" content-desc="Song B"/></node></hierarchy>"""
        self.assertIsNone(VERIFIER.focused_program_info(hierarchy, "Song A"))

    def test_counts_audio_only_and_seekable_provider_opens(self):
        log = "\n".join((
            "TV preview audio-only open request=1 key=abc bytes=100",
            "TV preview seekable cache hit request=2 key=def bytes=200",
            "TV preview seekable prepared open request=3 key=ghi bytes=300",
            "TV preview selected program title=Other contentIdHash=xyz",
        ))
        self.assertEqual(3, VERIFIER.count_preview_provider_opens(log))


if __name__ == "__main__":
    unittest.main()
