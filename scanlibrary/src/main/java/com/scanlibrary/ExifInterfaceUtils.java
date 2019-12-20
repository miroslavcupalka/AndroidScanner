package com.scanlibrary;

import android.support.media.ExifInterface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExifInterfaceUtils {

    public static void copyExifAttribute(ExifInterface source, ExifInterface target, String tag) {
        String value = source.getAttribute(tag);
        if (value == null) {
            return;
        }
        target.setAttribute(tag, value);
    }

    public static void copyExifAttributes(ExifInterface source, ExifInterface target, String... tags) {
        for (String tag : tags) {
            copyExifAttribute(source, target, tag);
        }
    }

    public static void copyExifAttributes(ExifInterface source, ExifInterface target, List<String> tags) {
        for (String tag : tags) {
            copyExifAttribute(source, target, tag);
        }
    }

    public static void removeTag(ExifInterface exif, String tag) {
        exif.setAttribute(tag, null);
    }

    public static void copyExifInterface(ExifInterface sourceExif, ExifInterface targetExif, String excludeExifTag) {
        final String[] EXIF_ATTRIBUTES_TO_COPY = {
                ExifInterface.TAG_DATETIME,
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_FLASH,
                ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_GPS_LATITUDE_REF,
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                ExifInterface.TAG_EXPOSURE_TIME,
                ExifInterface.TAG_F_NUMBER,
                ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
                ExifInterface.TAG_GPS_ALTITUDE,
                ExifInterface.TAG_GPS_ALTITUDE_REF,
                ExifInterface.TAG_GPS_TIMESTAMP,
                ExifInterface.TAG_GPS_DATESTAMP,
                ExifInterface.TAG_WHITE_BALANCE,
                ExifInterface.TAG_GPS_PROCESSING_METHOD,
                ExifInterface.TAG_ORIENTATION
        };

        List<String> exifAttributesToCopyList = new ArrayList<>(Arrays.asList(EXIF_ATTRIBUTES_TO_COPY));
        if (!Utils.isStringEmpty(excludeExifTag)) {
            exifAttributesToCopyList.remove(excludeExifTag);
        }

        ExifInterfaceUtils.copyExifAttributes(sourceExif, targetExif, exifAttributesToCopyList);

        try {
            targetExif.saveAttributes();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
