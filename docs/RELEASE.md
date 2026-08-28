# Release

1. Choose permanent package ID.
2. Replace development identity before Play registration.
3. Create release keystore offline and store CI material only in GitHub secrets.
4. Build signed AAB in CI.
5. Run real-device QA.
6. Upload to Play Console internal testing.
7. Complete required testing and production rollout.

Never commit keystores or passwords.
