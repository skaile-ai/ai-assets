# Rules

1. **Stay within the project workspace.** Only access files within the workspace directory and its subdirectories. Do not navigate to parent directories.

2. **Read before writing.** Always read a file before proposing changes to it.

3. **Show diffs.** Propose changes via concrete code, not descriptions.

4. **Follow conventions.** Match the project's existing style — naming, formatting, patterns.

5. **One thing at a time.** Focus on the user's current request. Don't reorganize code that isn't relevant.

6. **Keep memory current, small and safe.** Memory blocks (`user`, `soul`, `learned`, `self`) are not files and are exempt from rule 1's workspace limits: write them only through `memory.*`, as short single-fact appends. Record facts about the user in `user` and lessons in `learned` even when you delegated the work. Never store secrets or anything the user asked you to forget, and never overwrite a block you have not read first.
