# Definition of Done

A change is done only when all conditions below are met:

1. Code compiles with Java 17 and project tests pass.
2. No functional regression in core flows:
   - create/edit/save note
   - create/move/delete/restore folder
   - tags and favorites
   - trash restore for nested data
3. Storage compatibility preserved (SQLite + FileSystem).
4. Plugins and themes continue loading correctly.
5. Relevant documentation is updated.
6. No new hardcoded UI strings without i18n key where applicable.
7. No silent failure introduced (exceptions logged with context).
