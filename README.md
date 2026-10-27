# SP World — GPS Photo & Video Map Gallery

Android app in Gujarati. Select photos/videos using Android's system file picker, display items with GPS coordinates on an OpenStreetMap map, manually assign a location to items without GPS, and play a location-based slideshow.

## Features
- Gujarati interface
- OpenStreetMap via osmdroid
- Pick multiple image/video files; no broad media-library permission required
- Reads GPS from image EXIF when available
- Attempts to read embedded location metadata from video files
- Tap a map marker to view the media associated with that location
- Manual assignment: tap **મેન્યુઅલ લોકેશન**, select a media item, then tap the map to assign it
- Slideshow: images change every 6 seconds; videos play and then advance
- Place label and location icon overlay in slideshow
- Location assignments stored locally in app preferences; media itself is not uploaded

## Build APK with GitHub Actions
1. Create a GitHub repository named `SP-World`.
2. Upload the complete contents of this folder to the repository root.
3. Open **Actions** and enable workflows if GitHub asks.
4. The workflow **Build SP World APK** runs on push and can also be started manually.
5. Open the completed workflow run, then download the `SP-World-debug-apk` artifact.
6. Unzip the artifact and install `app-debug.apk` on your Android phone. Android may ask you to allow installs from your browser/file manager.

## Important notes
- This starter project has not been built or tested on your actual phone from this chat. GitHub Actions is the build verification step.
- Video GPS metadata varies by device and format. If it is unavailable, use manual assignment.
- Map tiles require an internet connection. Previously selected media and saved manual coordinates remain local.
- OpenStreetMap data attribution is shown in the app. Respect the OpenStreetMap tile usage policy; do not bulk-download tiles.
- Slideshow video playback uses Android's VideoView. The 6-second timer applies to images; videos advance when playback completes.
- For images/videos selected through Android's document picker, access can depend on the provider retaining URI permissions. Re-select files if a provider revokes access.
