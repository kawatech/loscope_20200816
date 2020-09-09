/*
 * MIT License
 *
 * Copyright (c) 2015 Douglas Nassif Roma Junior
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.losteaka.no001.oscilloscopeapp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothService
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.*

/**
 * Created by douglas on 26/05/17.
 */
class BitmapActivity : AppCompatActivity(), View.OnClickListener {
    //    private FloatingActionButton mFab;
    private var mImgOriginal: ImageView? = null

    //    private ImageView mImgBlackWhite;
    private var mService: BluetoothService? = null
    private val imageBitmap: Bitmap? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bitmap)

        /*
        setTitle("Losteaka Oscilloscope");
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        */
        // メニューバーにタイトル表示はやめる
        //    setSupportActionBar("Losteaka Oscilloscope");


        // FloatingActionButtonは削除、xmlからも削除
        //      mFab = (FloatingActionButton) findViewById(R.id.fab);
        //      mFab.setOnClickListener(this);
        mService = BluetoothService.getDefaultInstance()
        mImgOriginal = findViewById<View>(R.id.img_original) as ImageView
        //      mImgBlackWhite = (ImageView) findViewById(R.id.img_blackwhite);
        object : Thread() {
            override fun run() {
                //        final Bitmap original = BitmapFactory.decodeResource(getResources(), R.drawable.bmw);
                val original = BitmapFactory.decodeResource(resources, R.drawable.losteaka) // Losteakaのロゴ
                /*
                final Bitmap resized = Bitmap.createScaledBitmap(original, 255, 255, false);

                final Bitmap editedBrightness = BitmapHelper.changeBitmapContrastBrightness(resized, 1, 50);

                resized.recycle();

                imageBitmap = editedBrightness;

                final Bitmap editedGray = BitmapHelper.changeBitmapBlackWhite(editedBrightness);
*/
                // 画像の横、縦サイズを取得
                /*
                int imageWidth = original.getWidth();
                int imageHeight = original.getHeight();
                // Matrix インスタンス生成
                Matrix matrix = new Matrix();
*/

                //　別な方法で画面の向きを取得する。これもうまくいかない
                /* ----------------------------------
                Resources resources = getResources();
                Configuration config = resources.getConfiguration();
                switch(config.orientation) {
                    case Configuration.ORIENTATION_PORTRAIT:        // 縦
                        matrix.setRotate(0, imageWidth / 2, imageHeight / 2);
                     break;
                    case Configuration.ORIENTATION_LANDSCAPE:        // 横
                         matrix.setRotate(90, imageWidth / 2, imageHeight / 2);
                    break;
                }
      ------------------- */

                // 仕方がないので、横向きに固定する。

/*
                // 画像中心を基点に90度回転、実行はできるが検出はされないぞ
                // 横向きの場合
                // 端末の向きを取得
                // AndroidManifest.xml でandroid:screenOrientation="landscape"にして
                // そのままで横向きに固定する。よって、matrixでの回転はしない。

                int orientation = getResources().getConfiguration().orientation;
             //   if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    matrix.setRotate(90, imageWidth / 2, imageHeight / 2);
                }
                else {
                    matrix.setRotate(0, imageWidth / 2, imageHeight / 2);
                }

                // 90度回転したBitmap画像を生成
  //      final        Bitmap bitmap2 = Bitmap.createBitmap(original, 0, 0,
  //                      imageWidth, imageHeight, matrix, true);
*/
                runOnUiThread {
                    mImgOriginal!!.setImageBitmap(original)
                    //      mImgOriginal.setImageBitmap(bitmap2);             // 回転した画像は表示しない


                    //      mImgBlackWhite.setImageBitmap(editedGray);
                }
            }
        }.start()

        // 2秒で前の画面に戻るが、また繰り返す。Losで1を返す、これでスルーさせる
        /* ----------------------------------------*/
        val task: TimerTask = object : TimerTask() {
            override fun run() {

                //       Intent intent2 = new Intent( BitmapActivity.this, MainActivity.class );
                val intent = Intent(this@BitmapActivity, MainActivity::class.java)
                intent.putExtra("Los", 1) // 戻るときにLosに１を入れる
                startActivity(intent)
            }
        }
        val timer = Timer()
        timer.schedule(task, 2000) // 2秒待つ

        /*--------------------------------------------------   */
/*
        try {
            Thread.sleep(2000);
            Intent intent = new Intent( BitmapActivity.this, MainActivity.class );
            startActivity( intent );
        } catch (Exception e) {
            e.printStackTrace();
        }
        */

        /*
        val buttonSave = findViewById<View>(R.id.save_btn) as Button
        buttonSave.setOnClickListener {
            val filePath = Environment.getExternalStorageDirectory().toString() + "/memo.txt"
            val file = File(filePath)
            file.parentFile.mkdir()
            val fos: FileOutputStream
            try {
                fos = FileOutputStream(file, true)
                val osw = OutputStreamWriter(fos, "UTF-8")
                val bw = BufferedWriter(osw)
                val str = (findViewById<View>(R.id.EditTextInput) as EditText).text.toString()
                bw.write(str)
                bw.flush()
                bw.close()
            } catch (e: Exception) {
            }
        }

         */
    }

    override fun onClick(v: View) {
        object : Thread() {
            override fun run() {
                try {
                    //    byte[] bytes = WPXUtils.decodeBitmap(imageBitmap);
                    //   mService.write(bytes);


                    // 遷移先のActivityを指定して、Intentを作成する
                    val intent = Intent(this@BitmapActivity, MainActivity::class.java)

                    // 遷移先のアクティビティを起動させる
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }.start()
    }
}