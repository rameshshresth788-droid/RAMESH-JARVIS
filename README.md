# RAMESH JARVIS — Full Working Foundation

GitHub/Android Studio compatible Android app.

## Included
- Golden + white futuristic JARVIS system window
- Real Android SpeechRecognizer microphone input
- Android Text-to-Speech voice output
- Settings screen for AI endpoint, API key, model and voice settings
- API key stored locally using Android Keystore + AES-GCM encryption
- OpenAI-compatible HTTP chat API using HttpURLConnection (no networking library required)
- Demo/local response when AI is not configured
- Conversation history in memory for the current session
- Stop speaking button
- GitHub Actions debug APK workflow
- No API keys or secrets in source code

## AI settings
The app supports an OpenAI-compatible JSON chat endpoint.

Defaults:
Endpoint: https://api.openai.com/v1/chat/completions
Model: gpt-4o-mini

You may replace endpoint/model with another compatible provider.

## Important
An actual provider API key is never bundled in the APK. Enter it from the app Settings screen.

## Build
GitHub Actions:
Actions -> Build Debug APK -> Run workflow.

The generated APK is uploaded as a workflow artifact.
