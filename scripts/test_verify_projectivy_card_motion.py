"""Regression tests for the card-motion verifier's product/infra classification."""
import importlib.util
import unittest
from pathlib import Path

SPEC = importlib.util.spec_from_file_location(
    "verify_projectivy_card_motion",
    Path(__file__).with_name("verify-projectivy-card-motion.py"),
)
VERIFIER = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(VERIFIER)
classify_outcome = VERIFIER.classify_outcome


class CardMotionOutcomeTest(unittest.TestCase):
    def setUp(self):
        self.checks = {
            "projectivyFocused": True,
            "projectivyForeground": True,
            "focusedProgramInExpectedChannelRow": True,
            "pinkIndicatorVisibleInCard": True,
            "pinkIndicatorAnimatesInCard": True,
        }

    def test_missing_focus_is_inconclusive(self):
        self.checks["projectivyFocused"] = False
        self.assertEqual(classify_outcome(self.checks, provider_hit=False), "INCONCLUSIVE")

    def test_launcher_not_foreground_is_inconclusive(self):
        self.checks["projectivyForeground"] = False
        self.assertEqual(classify_outcome(self.checks, provider_hit=False), "INCONCLUSIVE")

    def test_missing_channel_row_with_focus_is_product_failure(self):
        self.checks["focusedProgramInExpectedChannelRow"] = False
        self.assertEqual(classify_outcome(self.checks, provider_hit=True), "FAIL_PRODUCT")

    def test_missing_animation_with_focus_is_product_failure(self):
        self.checks["pinkIndicatorAnimatesInCard"] = False
        self.assertEqual(classify_outcome(self.checks, provider_hit=True), "FAIL_PRODUCT")

    def test_unobserved_provider_request_is_inconclusive(self):
        self.assertEqual(classify_outcome(self.checks, provider_hit=False), "INCONCLUSIVE")

    def test_all_assertions_are_pass(self):
        self.assertEqual(classify_outcome(self.checks, provider_hit=True), "PASS")


if __name__ == "__main__":
    unittest.main()
