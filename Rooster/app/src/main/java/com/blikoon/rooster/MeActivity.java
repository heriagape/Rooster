package com.blikoon.rooster;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class MeActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView jidTextView;
    private ImageView profileImageView;
    private static final String TAG = "MeActivity";
    private static final int SELECT_PHOTO = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_me);

        jidTextView = (TextView) findViewById(R.id.jid_text);

        profileImageView = (ImageView) findViewById(R.id.profile_image);

        //Show the current image avatar if already available
        RoosterConnection rc = RoosterConnectionService.getRoosterConnection();
        if( rc != null)
        {
            byte[] ba = rc.getSelfAvatar();
            if ( ba != null)
            {
                Drawable image = new BitmapDrawable(getResources(),
                        BitmapFactory.decodeByteArray(ba, 0, ba.length));
                profileImageView.setImageDrawable(image);
            }
        }

        profileImageView.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        //When user clicks on the profile image
        Log.d(TAG,"Clicked on the profile picture");
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, SELECT_PHOTO);


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch(requestCode) {
            case SELECT_PHOTO:
                if(resultCode == RESULT_OK){
                    Log.d(TAG,"Result is OK");
                    Uri selectedImage = imageReturnedIntent.getData();

                    Bitmap bm = null;

                    try {
                       bm = decodeUri(selectedImage);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                    if( bm != null)
                    {
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bm.compress(Bitmap.CompressFormat.PNG, 0, stream);
                        byte[] byteArray = stream.toByteArray();
                        Log.d(TAG,"Bitmap not NULL, proceeding with setting image. The array size is :" +byteArray.length);
                        RoosterConnection rc = RoosterConnectionService.getRoosterConnection();
                        if ( rc != null) {
                            if (rc.setSelfAvatar(byteArray)) {
                                Log.d(TAG, "Avatar set correctly");
                            } else
                            {
                                Log.d(TAG,"Could not set user avatar");
                            }

                        }

                    }




                }else
                {
                    Log.d(TAG,"Canceled out the Image selection act");
                }
        }
    }

    private Bitmap decodeUri(Uri selectedImage) throws FileNotFoundException {

        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o);

        // The new size we want to scale to
        final int REQUIRED_SIZE = 140;

        // Find the correct scale value. It should be the power of 2.
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp / 2 < REQUIRED_SIZE
                    || height_tmp / 2 < REQUIRED_SIZE) {
                break;
            }
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o2);

    }
}
