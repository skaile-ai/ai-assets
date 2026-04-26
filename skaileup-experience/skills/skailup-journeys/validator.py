#!/usr/bin/env python3
"""Validator for the journeys skill.
Re-generate with: /compile-validators journeys
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[4] / "skaileup-shared" / "scripts"))
from validator_lib import Validator, main  # noqa: E402

SKILL = "journeys"
STORIES = "_concept/experience/journeys/stories.json"
SCHEMA = "skaileup-shared/contracts/stories_schema.json"


def _all_stories(v):
    """Extract flat list of all stories across all story maps."""
    data = v.read_json(STORIES)
    if not data or not isinstance(data, dict):
        return []
    return [s for m in data.get("story_maps", []) for s in m.get("stories", [])]


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    # ── MUST rules ──

    v.must("produce exactly one hero story map", lambda: (
        v.json_count(
            (v.read_json(STORIES) or {}).get("story_maps", []),
            lambda m: m.get("stage") == "hero",
            expected=1, op="eq",
        )
    ))

    def check_ears():
        stories = _all_stories(v)
        if not stories:
            return False, "No stories found in stories.json"
        for s in stories:
            ac = s.get("acceptance_criteria", [])
            if not ac:
                return False, f"Story '{s.get('id')}' has no acceptance criteria"
        return True, ""

    v.must("write EARS acceptance criteria for every story", check_ears)

    v.must("include personas in stories.json", lambda: (
        v.json_array_all_have(
            (v.read_json(STORIES) or {}).get("personas", []),
            "id",
            context="in personas",
        ) if (v.read_json(STORIES) or {}).get("personas") else (False, "No personas found")
    ))

    v.must("validate stories.json against schema", lambda: (
        v.json_schema_validate(STORIES, SCHEMA)
    ))

    def check_downstream():
        stories = _all_stories(v)
        if not stories:
            return False, "No stories found"
        for s in stories:
            ds = s.get("downstream", {})
            if not ds:
                return False, f"Story '{s.get('id')}' missing downstream hints"
            for field in ("candidate_features", "candidate_entities", "candidate_screens"):
                if field not in ds:
                    return False, f"Story '{s.get('id')}' missing downstream.{field}"
        return True, ""

    v.must("include downstream hints for every story", check_downstream)

    def check_status():
        stories = _all_stories(v)
        if not stories:
            return False, "No stories found"
        for s in stories:
            if s.get("status") != "proposed":
                return False, f"Story '{s.get('id')}' has status '{s.get('status')}', expected 'proposed'"
        return True, ""

    v.must("set status: proposed on all new stories", check_status)

    # ── NEVER rules ──

    v.never("define more than one hero story map", lambda: (
        v.json_count(
            (v.read_json(STORIES) or {}).get("story_maps", []),
            lambda m: m.get("stage") == "hero",
            expected=1, op="lte",
        )
    ))

    def check_no_empty_ac():
        stories = _all_stories(v)
        for s in stories:
            ac = s.get("acceptance_criteria")
            if ac is None or (isinstance(ac, list) and len(ac) == 0):
                return False, f"Story '{s.get('id')}' has no acceptance criteria"
        return True, ""

    v.never("skip acceptance criteria", check_no_empty_ac)

    # ── CHECKLIST ──

    v.checklist("brief.md was read and exists", lambda: (
        v.file_exists("_concept/discovery/brief.md")
    ))

    v.checklist("Personas defined with ids, labels, and goals", lambda: (
        v.json_array_all_have(
            (v.read_json(STORIES) or {}).get("personas", []),
            "goals",
            context="in personas",
        ) if (v.read_json(STORIES) or {}).get("personas") else (False, "No personas")
    ))

    v.checklist("Exactly one story map has stage: hero", lambda: (
        v.json_count(
            (v.read_json(STORIES) or {}).get("story_maps", []),
            lambda m: m.get("stage") == "hero",
            expected=1, op="eq",
        )
    ))

    v.checklist("Every story has at least one EARS acceptance criterion", check_ears)

    v.checklist("Every story has downstream hints", check_downstream)

    v.checklist("stories.json validates against schema", lambda: (
        v.json_schema_validate(STORIES, SCHEMA)
    ))

    def check_priority_dist():
        data = v.read_json(STORIES)
        if not data:
            return False, "No stories.json"
        for m in data.get("story_maps", []):
            stage = m.get("stage")
            for s in m.get("stories", []):
                prio = s.get("priority", "")
                if stage == "hero" and prio != "must":
                    return False, f"Hero story '{s.get('id')}' has priority '{prio}', expected 'must'"
                if stage == "backlog" and prio not in ("could", "wont"):
                    return False, f"Backlog story '{s.get('id')}' has priority '{prio}', expected 'could' or 'wont'"
        return True, ""

    v.checklist("Priority distribution: hero=must, backlog=could/wont", check_priority_dist)

    v.skip("Hero journey approved by user before mapping remaining journeys",
           rule_type="CHECKLIST", reason="process — cannot verify conversation flow")
    v.skip("Summary table shown and approved by user",
           rule_type="CHECKLIST", reason="process — cannot verify user interaction")
    v.skip("derive personas from brief audience and research",
           reason="semantic — content derivation quality")

    return v.result()


if __name__ == "__main__":
    main(validate)
