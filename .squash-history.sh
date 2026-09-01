#!/bin/bash
set -e
cd "$(dirname "$0")"

git checkout --orphan clean-main
git add -A
TREE=$(git write-tree)

export GIT_AUTHOR_NAME='Ian Mbae'
export GIT_AUTHOR_EMAIL='stewiegriffin3108ia@gmail.com'
export GIT_COMMITTER_NAME='Ian Mbae'
export GIT_COMMITTER_EMAIL='stewiegriffin3108ia@gmail.com'

MSG="TempBox v2.0.1 — disposable email for Android.

Instant throwaway inboxes with live polling, OTP detection, home screen widget, autofill support, and a dark neon UI. Built with Kotlin and Jetpack Compose."

COMMIT=$(echo "$MSG" | git commit-tree "$TREE")
git reset --hard "$COMMIT"
git branch -D main
git branch -m main

git log -1 --format='%H%n%an <%ae>%n%B'
