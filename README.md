# ScanLibrary
ScanLibrary is an android document scanning library built on top of OpenCV, using the app you will be able to select the exact edges and crop the document accordingly from the selected 4 edges and change the perspective transformation of the cropped image.

# Screenshots

<div align="center">

		<img width="23%" src="https://raw.githubusercontent.com/jhansireddy/AndroidScannerDemo/master/ScanDemoExample/screenshots/scanInput.png" alt="Scan Input" title="Scan Input"</img>
        <img height="0" width="8px">
        <img width="23%" src="https://raw.githubusercontent.com/jhansireddy/AndroidScannerDemo/master/ScanDemoExample/screenshots/scanPoints.png" alt="Scan Points" title="Scan Input"</img>
        <img height="0" width="8px">
        <img width="23%"src="https://raw.githubusercontent.com/jhansireddy/AndroidScannerDemo/master/ScanDemoExample/screenshots/blackWhiteScannedResult.png" alt="After Scan" title="After Scan"></img>
        <img height="0" width="8px">
        <img width="23%" src="https://raw.githubusercontent.com/jhansireddy/AndroidScannerDemo/master/ScanDemoExample/screenshots/returned_scan_result.png" alt="Scanned Result" title="Scanned Result"></img>
</div>

# Videos


<div align="center" >
<a href="http://www.youtube.com/watch?feature=player_embedded&v=Kl7rRZ79m6k" target="_blank"><img src="https://raw.githubusercontent.com/jhansireddy/AndroidScannerDemo/master/ScanDemoExample/screenshots/scanPoints.png" 
alt="Scan Video" width="40%" border="10" /></a>
</div>


# Using it in your project
- If you are using android studio, add the dependency to your main app build.gradle this way: 

```	    
-  compile 'com.github.andrejlukasevic:document-scanner:4.0.0'  // Check for latest version and replace version code
```
- In your activity or fragment when you want to give an option of document scanning to user then:
Start the scanlibrary ScanActivity, with this the app will go to library, below is the sample code snippet:
```java
        Intent intent = new Intent(this, ScanActivity.class);
        intent.putExtra(ScanActivity.EXTRA_BRAND_IMG_RES, R.drawable.ic_crop_white_24dp); // Set image for title icon - optional
        intent.putExtra(ScanActivity.EXTRA_TITLE, "Crop Document"); // Set title in action Bar - optional
        intent.putExtra(ScanActivity.EXTRA_ACTION_BAR_COLOR, R.color.green); // Set title color - optional
        intent.putExtra(ScanActivity.EXTRA_LANGUAGE, "en"); // Set language - optional
        startActivityForResult(intent, REQUEST_CODE_SCAN);
```

- Once the scanning is done, the application is returned from scan library to main app, to retrieve the scanned image, add onActivityResult in your activity or fragment from where you have started startActivityForResult, below is the sample code snippet:
```java
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCAN && resultCode == Activity.RESULT_OK) {
            String imgPath = data.getStringExtra(ScanActivity.RESULT_IMAGE_PATH);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap =  BitmapFactory.decodeFile(imgPath, options);
            viewHolder.image.setImageBitmap(bitmap);
        }
    }
```
