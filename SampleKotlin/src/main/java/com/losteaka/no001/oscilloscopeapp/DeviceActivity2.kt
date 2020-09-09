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

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaScannerConnection
import android.media.MediaScannerConnection.*
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothService
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothService.OnBluetoothEventCallback
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothStatus
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothWriter
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.android.synthetic.main.activity_device.*
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import kotlin.experimental.and


/**
 * Created by douglas on 10/04/2017.
 */
class DeviceActivity2 : AppCompatActivity(), OnBluetoothEventCallback, View.OnClickListener {
    //    private var mFab: FloatingActionButton? = null
//    private var mEdRead: EditText? = null
    private var mEdRead: TextView? = null                  // kawa TextView に変更
    private var mEdWrite: EditText? = null                  // kawa 削除した
    private var mService: BluetoothService? = null
    private var mWriter: BluetoothWriter? = null
    // kawa
    private val hexArray = "0123456789ABCDEF".toCharArray()
    private var mChart: LineChart? = null           // kawa2
    private var mChart2: LineChart? = null           // kawa3

    private var offset = 9999.toInt()
    private var restBuffer: ByteArray = byteArrayOf(0x02.toByte(), 0x02.toByte(), 0x02.toByte(), 0x02.toByte(), 0x02.toByte())
    private var prevVer =0.toInt()

    private lateinit var mToolbar: Toolbar              // kawa Drawer
    private var mGenre = 0

    private var xLenght = 1000f
    private var loopCount = 1f

    // 不連続時に1回前の値を代用するために、保存しておくための変数
    private var prevf1 = 0f
    private var prevf2 = 0f

    // X軸のラベルの間隔
    private var xIntervalRange = 100

    // FileSave
    private val REQUEST_PERMISSION = 1000
    private var file: File? = null
    private var saveCount = 0L
    private var saveOffset = 0
    private var saveBufSize = 13125
    var DataArray = Array(saveBufSize, {arrayOfNulls<Short>(8)})        // Float => Short

    // 非同期
    private var progressDialog: ProgressDialog? = null
    var mHandler: Handler? = null

    // 画面を開くときの処理
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device2)

        // SAVEボタン、デフォルトは無効
        save_btn.isEnabled = false

        // FileSave Android 6, API 23以上でパーミッシンの確認
        if (Build.VERSION.SDK_INT >= 23) {
            checkPermission()
        } else {
            setUpReadWriteExternalStorage()
        }


        // kawa Drawer これがあると停止する
        mToolbar = findViewById(R.id.toolbar)
        //      setSupportActionBar(mToolbar)

/* ----------------------------------------------------------------------------------------------
         Mainから秒数の設定データをもらう。キーワードがXSECでvaluew1に値が入る
         この値によって30秒ごとにX軸の幅を設定する。メニューバーに秒数表示。
 ------------------------------------------------------------------------------------------------- */
        val value1 = intent.getIntExtra("XSEC",1)

        val dm = DisplayMetrics()
        getWindowManager().getDefaultDisplay().getMetrics(dm)
        var winW = (dm.widthPixels )
        var winH = dm.heightPixels
        var wLong = 1
        if(winW > winH) {
            wLong = winW
        }
        else {
            wLong = winH
        }

     //   wLong = (wLong - 840) / 7               // シャープのスマホで見えなくする
           wLong = (wLong - 600) / 22            // 全部入るのはこれ


        var stmp = ""
        for (i in 0..wLong) {
            stmp = stmp + " "
        }

        if (value1 == 1 ) {
            mToolbar.title = "Losteaka Oscilloscope 30sec " + stmp + "ロステーカ株式会社"
            xLenght = 1875f
            xIntervalRange = 100
            //    xLenght = 187f                 // 試しに幅を 1/10　にするとき
        }
        else if (value1 == 2) {
            mToolbar.title = "Losteaka Oscilloscope 60sec " + stmp + "ロステーカ株式会社"
            xLenght = 3750f
            xIntervalRange = 200
        }
        else if (value1 == 3) {
            mToolbar.title = "Losteaka Oscilloscope 90sec " + stmp + "ロステーカ株式会社"
            xLenght = 5625f
            xIntervalRange = 300
        }
        else if (value1 == 4) {
            mToolbar.title = "Losteaka Oscilloscope 120sec" + stmp + "ロステーカ株式会社"
            xLenght = 7500f
            xIntervalRange = 400
        }
        else if (value1 == 5) {
            mToolbar.title = "Losteaka Oscilloscope 150sec" + stmp + "ロステーカ株式会社"
            xLenght = 9375f
            xIntervalRange = 500
        }
        else if (value1 == 6) {
            mToolbar.title = "Losteaka Oscilloscope 180sec" + stmp + "ロステーカ株式会社"
            xLenght = 11250f
            xIntervalRange = 600
        }
        else if (value1 == 7) {
            mToolbar.title = "Losteaka Oscilloscope 210sec" + stmp + "ロステーカ株式会社"
            xLenght = 13125f
            xIntervalRange = 700
        }
        else if (value1 == 8) {
            mToolbar.title = "Losteaka Oscilloscope 15sec " + stmp + "ロステーカ株式会社"
            xLenght = 938f
            xIntervalRange = 50
        }
        setSupportActionBar(mToolbar)

        saveBufSize = xLenght.toInt()       // SAVEするときのデータ数を秒数で決める

        /* --------------- kawa2 ---------------------------
            グラフ（チャート）の初期設定、2段で2つ分
        ---------------------------------------------------- */
        mChart = findViewById(R.id.chart) as LineChart
        initChart()

        mChart2 = findViewById(R.id.chart2) as LineChart            // kawa3
        initChart2()

        // DataArray[][]を初期化
        for (i in 0..saveBufSize-1) {
            for(j in 0..7) {
                DataArray[i][j] = 0
            }
        }


        //      mEdRead = findViewById<View>(R.id.ed_read) as EditText
        mEdRead = findViewById<View>(R.id.ed_read) as TextView                  // kawa EditからTextView に変更

        //       mEdWrite = findViewById<View>(R.id.ed_write) as EditText          // 送信することはないのでマスクする
        mService = BluetoothService.getDefaultInstance()
        mWriter = BluetoothWriter(mService)


        /* --------------------------------------------------
           ボタンをタッチすると、MainActivityの画面に戻る
           MainActivityでX軸の幅を設定するようにした
           SAVEボタン、測定後有効、測定中は無効
        ---------------------------------------------------- */
        val btn1: Button = findViewById(R.id.backr_btn)
        btn1.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("Los", 2)
            startActivity(intent)
        }

        // ボタンをタッチするたびに表示を変える
        start_btn.setOnClickListener() {
            if (start_btn.text == "START") {
                start_btn.text ="STOP"
                save_btn.isEnabled = false

                // FileSave カウンタをクリアして、配列もクリアする
                saveCount = 0L
                saveOffset = 0
                for (i in 1..saveBufSize) {
                    for(j in 0..7) {
                        DataArray[i-1][j] = 0           // 0f => 0
                        }
                    }

            }
            else {
                start_btn.text ="START"
                save_btn.isEnabled = true
            }
        }


        // 非同期
        //ハンドラを生成
        mHandler = Handler()
        val buttonSave = findViewById<Button>(R.id.save_btn)

        //ProgressDialogを生成します。
        progressDialog = ProgressDialog(this)
        progressDialog!!.setMessage("Saving")

        //buttonがクリックされた時の処理を登録します。
        buttonSave!!.setOnClickListener { buttonProcess() }

    }       // ================= onCreat()ここまで ============================


    // 非同期

    /**
     * buttonがクリックされた時の処理
     */
    private fun buttonProcess() {
        //ProgressDialogを表示します。
        progressDialog!!.show()

        //スレッドを生成して起動します。
        val thread = MyThread()
        thread.start()
    }



    internal inner class MyThread() : Thread() {
        override fun run() {
            //時間のかかる処理実行します。

            // 現在ストレージが書き込みできるかチェック
            if (isExternalStorageWritable) {
                var text: String? = null

                //   runOnUiThread {
                //       Toast.makeText(applicationContext, "Saved", Toast.LENGTH_SHORT).show()
                //       save_btn.text = "SAVING"
                //   }


// ファイル名を年月日時間分秒とする。
                val date = Date()
                val format = SimpleDateFormat("YYMMddHHmmss")           // ddは小文字
                val stmp9 = format.format(date).toString()

                file = File(getExternalFilesDir("/"), "data_" + stmp9 + ".csv")


                for (i in 1..saveBufSize) {

                    // int型の二次元配列の1行分の要素を取り出して、文字列にしてカンマで区切る。最後は改行
                    // メインのデータ2個、I/O0からI/O5までの6個で計7個
                    // これを　文字列str　にする
                    var str = ""
                    var saveIdx = (i + saveOffset - 1) % saveBufSize

                    for (j in 0..7) {
                        if (saveCount >= saveBufSize.toLong()) {      // バッファがリング1周を超えたとき
                            if (j == 7) {
                                str = str + DataArray[saveIdx][j].toString() + "\n"
                            } else {
                                str = str + DataArray[saveIdx][j].toString() + ","
                            }
                        } else {      // 1周超えていない
                            if (j == 7) {
                                str = str + DataArray[i - 1][j].toString() + "\n"
                            } else {
                                str = str + DataArray[i - 1][j].toString() + ","
                            }

                        }
                    }         // end of for j


                    Log.d(TAG, "SAVE: "+ saveIdx +": " + DataArray[saveIdx][0])

                    // PCから見えるようにするために必要な処理
                    MediaScannerConnection.scanFile(applicationContext, arrayOf(file!!.absolutePath), null, null)
                    try {

                        //           FileOutputStream(file, false).use { fileOutputStream ->                                 // falseなら追記しない
                        FileOutputStream(file, true).use { fileOutputStream ->
                            OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8).use { outputStreamWriter ->
                                BufferedWriter(outputStreamWriter).use { bw ->
                                    bw.write(str)
                                    bw.flush()
                                    text = "saved"
                                }
                            }
                        }
                    } catch (e: Exception) {
                        text = "error: FileOutputStream"
                        e.printStackTrace()
                    }

                }          // end of for i

                // ここにtoastがあると、アプリが停止する。
                //    val toast = Toast.makeText(this, "保存しました", Toast.LENGTH_LONG)
                //   toast.show()
                //   Toast.makeText(applicationContext, "Saved2", Toast.LENGTH_SHORT).show()
                //   save_btn.text = "SAVE/RECALL/DEL"

            }  // isExternalStorageWritable ここまで


            //メインスレッドのメッセージキューにメッセージを登録します。
            mHandler!!.post(Runnable
            //run()の中の処理はメインスレッドで動作されます。
            { //画面のtextViewへ"処理が完了しました。"を表示させる。
                //    textView!!.text = "処理が完了しました。"

                //ProgressDialogを消去します。
                progressDialog!!.dismiss()
            })
        }          // override fun run() ここまで
    }  // internal inner class MyThread() ここまで

// 非同期追懐ここまで





    // FileSave
    private fun setUpReadWriteExternalStorage() {

        /* --------------------------------
        // Read は当面マスクしておく
             val buttonRead = findViewById<Button>(R.id.button_read)
             buttonRead.setOnClickListener {
                 var str: String? = null

                 // 現在ストレージが読出しできるかチェック
                 if (isExternalStorageReadable) {
                     try {
                         FileInputStream(file).use { fileInputStream ->
                             InputStreamReader(fileInputStream, StandardCharsets.UTF_8).use { inputStreamReader ->
                                 BufferedReader(inputStreamReader).use { reader ->
                                     var lineBuffer: String?
                                     while (reader.readLine().also { lineBuffer = it } != null) {
                                         str = lineBuffer
                                     }
                                 }
                             }
                         }
                     } catch (e: Exception) {
                         str = "error: FileInputStream"
                         e.printStackTrace()
                     }
                 }
              //   textView?.setText(str)
             }
             ---------------------------------------------- */

    }
//             --------- end of setUpReadWriteExternalStorage --------------------------


    /* Checks if external storage is available for read and write */
    val isExternalStorageWritable: Boolean
        get() {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state
        }

    /* Checks if external storage is available to at least read */
    /* ------------------------------------------------------------------------
    val isExternalStorageReadable: Boolean
        get() {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state
        }
--------------------------------------------------------------------------------------- */

    // permissionの確認
    fun checkPermission() {
        // 既に許可している
        if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED) {
            setUpReadWriteExternalStorage()
        } else {
            requestLocationPermission()
        }
    }

    // 許可を求める
    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this@DeviceActivity2, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_PERMISSION)
        } else {
            val toast = Toast.makeText(this, "アプリ実行に許可が必要です", Toast.LENGTH_SHORT)
            toast.show()
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_PERMISSION)
        }
    }

    // 結果の受け取り
    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_PERMISSION) {
            // 使用が許可された
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setUpReadWriteExternalStorage()
            } else {
                // それでも拒否された時の対応
                val toast = Toast.makeText(this, "何もできません", Toast.LENGTH_SHORT)
                toast.show()
            }
        }
    }


// ================== FileSave ここまで ==============================================================








    override fun onResume() {
        super.onResume()
        mService!!.setOnEventCallback(this)

    }


    /*  ------------------------------------ kawa2 -----------------
    チャートの初期設定、MPAndroidChartSampleから流用
    ----------------------------------------------------------------- */

    private fun initChart() {
        // no description text
        mChart?.setDescription("")
        mChart?.setNoDataTextDescription("You need to provide data for the chart.")

        // enable touch gestures
        mChart?.setTouchEnabled(true)

        // enable scaling and dragging
        mChart?.setDragEnabled(true)
        mChart?.setScaleEnabled(true)
        mChart?.setDrawGridBackground(false)


        // if disabled, scaling can be done on x- and y-axis separately
        mChart?.setPinchZoom(true)


        // set an alternative background color
        //   mChart?.setBackgroundColor(Color.LTGRAY)
        mChart?.setBackgroundColor(Color.BLACK)
        val data = LineData()
        //   data.setValueTextColor(Color.BLACK)
        data.setValueTextColor(Color.LTGRAY)

        // add empty data
        mChart?.setData(data)

        //  ラインの凡例の設定
        val l = mChart?.getLegend()
        l?.form = Legend.LegendForm.LINE        // 凡例を線で表す
        //    l?.textColor = Color.BLACK
        l?.textColor = Color.LTGRAY

        val xl = mChart?.getXAxis()
        //   xl?.textColor = Color.BLACK
        xl?.textColor = Color.LTGRAY
        xl?.setLabelsToSkip(xIntervalRange)

        xl?.isEnabled = true                   // falseのとき、上のラベルが表示されない

        val leftAxis = mChart?.getAxisLeft()
        //   leftAxis?.textColor = Color.BLACK
        leftAxis?.textColor = Color.LTGRAY
        leftAxis?.setAxisMaxValue( 5000.0f)             // Y軸の最大、最小
        leftAxis?.setAxisMinValue(-3.0f)

        // リミットラインを入れる、やめる
        //   leftAxis?.setDrawLimitLinesBehindData(true)           // グラフの線の後ろにするとき
        /* -----------------------------------------
        val ll = LimitLine(750f,"lower")
        ll.lineColor = Color.parseColor("#008577")        // 濃い緑
        ll.lineWidth = 1f
        ll.textColor = Color.BLACK
        ll.textSize = 10f
        leftAxis?.addLimitLine(ll)

        val uu = LimitLine(3500f,"upper")
        uu.lineColor = Color.CYAN                   // 空色
        uu.lineWidth = 1f
        uu.textColor = Color.BLACK
        uu.textSize = 10f
        leftAxis?.addLimitLine(uu)
    ------------------------------------------------------------- */

        //     leftAxis?.setStartAtZero(false)
        leftAxis?.setDrawGridLines(true)
        val rightAxis = mChart?.getAxisRight()
        rightAxis?.isEnabled = false
    }

    // ---------------------------------------------------------
    private fun initChart2() {
        // no description text
        mChart2?.setDescription("")
        mChart2?.setNoDataTextDescription("You need to provide data for the chart.")

        // enable touch gestures
        mChart2?.setTouchEnabled(true)

        // enable scaling and dragging
        mChart2?.setDragEnabled(true)
        mChart2?.setScaleEnabled(true)
        mChart2?.setDrawGridBackground(false)

        // if disabled, scaling can be done on x- and y-axis separately
        mChart2?.setPinchZoom(true)

        // set an alternative background color
        //   mChart2?.setBackgroundColor(Color.LTGRAY)
        mChart2?.setBackgroundColor(Color.BLACK)
        val data2 = LineData()                      // kawa2
        //   data2.setValueTextColor(Color.BLACK)
        data2.setValueTextColor(Color.LTGRAY)

        // add empty data
        mChart2?.setData(data2)

        //  ラインの凡例の設定
        val l = mChart2?.getLegend()
        l?.form = Legend.LegendForm.LINE
        //   l?.textColor = Color.BLACK
        l?.textColor = Color.LTGRAY
        val xl = mChart2?.getXAxis()
        //   xl?.textColor = Color.BLACK
        xl?.textColor = Color.LTGRAY
        xl?.setLabelsToSkip(xIntervalRange)

        xl?.isEnabled = false                   // falseのとき、上のラベルが表示されない

        val leftAxis = mChart2?.getAxisLeft()
        //    leftAxis?.textColor = Color.BLACK
        leftAxis?.textColor = Color.LTGRAY

        //   leftAxis?.setAxisMaxValue( 100.0f)
        //   leftAxis?.setAxisMinValue(-10.0f)

        // Y軸の幅を明示的に1.0にする
        leftAxis?.setLabelCount(6,true)         // Y軸のラベルを6個
        leftAxis?.setGranularity(1.0f)                           // 間隔は1.0

        leftAxis?.setAxisMaxValue( 6.0f)
        leftAxis?.setAxisMinValue(0.0f)
        leftAxis?.setShowOnlyMinMax(true)

        //     leftAxis?.setStartAtZero(false)
        leftAxis?.setDrawGridLines(true)
        val rightAxis = mChart2?.getAxisRight()
        rightAxis?.isEnabled = false
    }
// -------------------------------------------------------------------------





    /*  ----------------------------------------------------------------------------
        // ここが受信データが入ったときに実行される関数、この中で処理する
     ------------------------------------------------------------------------------- */
    override fun onDataRead(buffer: ByteArray, length: Int) {


// kawa このループの中で何かする


        // ここでグラフにデータ nn を送る
        //  追加描画するデータを追加
        val data = mChart?.getData() ?: return
        val data2 = mChart2?.getData() ?: return
        //   var set = data.getDataSetByIndex(0)

        //   if(data != null){
//リアルタイムでデータを更新する場合はILineDataSetを使う
        //データをセットする際にインデックスを指定

        //リアルタイムでデータを更新する場合はILineDataSetを使う
        //データをセットする際にインデックスを指定
        // data, data2でそれぞれ0から始める
        var set1 = data.getDataSetByIndex(0)
        //2本目のグラフ（インデックスを1に）
        var set2 = data.getDataSetByIndex(1)       /////

        // 下のグラフに描く、dsta2でインデックスは0から
        var set20 = data2.getDataSetByIndex(0)
        var set21 = data2.getDataSetByIndex(1)
        var set22 = data2.getDataSetByIndex(2)
        var set23 = data2.getDataSetByIndex(3)
        var set24 = data2.getDataSetByIndex(4)
        var set25 = data2.getDataSetByIndex(5)

        //   }


        if (set1 == null) {
            //      set1 = createSet()
            //      set2 = createSet()
            set1 = LineDataSet(null, "データ1")
            //   set1.color = Color.BLUE
            set1.color = Color.YELLOW
            set1.setDrawValues(false)
            set1.setDrawCircles(false)          // データの頂点の丸を描画しない
            data.addDataSet(set1)
        }

        if (set2 == null) {
            set2 = LineDataSet(null, "データ2")
            //    set2.color = Color.RED
            //    set2.color = Color.GREEN
            //    set2.color = Color.parseColor("#00FF5F")            // 鮮やかな緑
            set2.color = Color.parseColor("#00FF00")            // 鮮やかな緑
            set2.setDrawValues(false)
            set2.setDrawCircles(false)
            data.addDataSet(set2)  /////
        }

        /* -------------------------------------------------------------------
        IO0からio5を準備する
        ---------------------------------------------------------------------- */
        if (set20 == null) {
            set20 = LineDataSet(null, "IO0")        // kawa3
            //   set3.color = Color.GREEN                      // 緑
            //    set3.color = Color.BLACK
            //      set3.color = Color.MAGENTA                  // 桃色
            //      set3.color = Color.CYAN                     // 空色 //
            //      set3.color = Color.parseColor("#008577")        // 濃い緑
            //   set3.color = Color.parseColor("#d2691e")    //チョコレート色
            set20.color = Color.YELLOW
            set20.setDrawValues(false)
            set20.setDrawCircles(false)
            data2.addDataSet(set20)
        }

        if (set21 == null) {
            set21 = LineDataSet(null, "io1")        // kawa3
            //   set22.color = Color.GREEN                      // 緑
            //    set22.color = Color.BLACK
            //      set22.color = Color.MAGENTA                  // 桃色
            //      set22.color = Color.CYAN                     // 空色 //
            //      set22.color = Color.parseColor("#008577")        // 濃い緑
           //    set22.color = Color.parseColor("#d2691e")    //チョコレート色
            set21.color = Color.parseColor("#ffa500")    //オレンジ
         //   set22.color = Color.YELLOW
            set21.setDrawValues(false)
            set21.setDrawCircles(false)
            data2.addDataSet(set21)
        }

        if (set22 == null) {
            set22 = LineDataSet(null, "io2")        // kawa3
            //   set22.color = Color.GREEN                      // 緑
            //    set22.color = Color.BLACK
            set22.color = Color.MAGENTA                  // 桃色
            //      set22.color = Color.CYAN                     // 空色 //
            //      set22.color = Color.parseColor("#008577")        // 濃い緑
            //   set22.color = Color.parseColor("#d2691e")    //チョコレート色
         //   set22.color = Color.YELLOW
            set22.setDrawValues(false)
            set22.setDrawCircles(false)
            data2.addDataSet(set22)
        }


        if (set23 == null) {
            set23 = LineDataSet(null, "io3")        // kawa3
            //   set22.color = Color.GREEN                      // 緑
            //    set22.color = Color.BLACK
         //   set24.color = Color.MAGENTA                  // 桃色
            set23.color = Color.CYAN                     // 空色 //
            //      set22.color = Color.parseColor("#008577")        // 濃い緑
            //   set22.color = Color.parseColor("#d2691e")    //チョコレート色
            //   set22.color = Color.YELLOW
         //   set23.setDrawValues(false)
            set23.setDrawCircles(false)
            data2.addDataSet(set23)
        }


        if (set24 == null) {
            set24 = LineDataSet(null, "io4")        // kawa3
            //   set22.color = Color.GREEN                      // 緑
            //    set22.color = Color.BLACK
            //   set24.color = Color.MAGENTA                  // 桃色
         //   set25.color = Color.CYAN                     // 空色 //
            set24.color = Color.RED                     // 赤
            //      set22.color = Color.parseColor("#008577")        // 濃い緑
            //   set22.color = Color.parseColor("#d2691e")    //チョコレート色
            //   set22.color = Color.YELLOW
            //   set23.setDrawValues(false)
            set24.setDrawCircles(false)
            data2.addDataSet(set24)
        }


        if (set25 == null) {
            set25 = LineDataSet(null, "io5")        // kawa3
            set25.color = Color.GREEN                      // 緑
            //    set22.color = Color.BLACK
            //   set24.color = Color.MAGENTA                  // 桃色
            //   set25.color = Color.CYAN                     // 空色 //
         //   set25.color = Color.RED                     // 赤
            //      set22.color = Color.parseColor("#008577")        // 濃い緑
            //   set22.color = Color.parseColor("#d2691e")    //チョコレート色
            //   set22.color = Color.YELLOW
            //   set23.setDrawValues(false)
            set25.setDrawCircles(false)
            data2.addDataSet(set25)
        }



// STARTボタンをタップしたら表示する
        if (start_btn.text == "STOP") {
            val date = Date()
            val format = SimpleDateFormat("HH:mm:ss")
            data.addXValue(format.format(date))
            data2.addXValue(format.format(date))
        }

        // 初期値を設定しておく
        var fvalue1 = 5000f
        var fvalue2 = 5000f

        var fvalue20 = 5.0f
        var fvalue21 = 4.0f
        var fvalue22 = 3.0f
        var fvalue23 = 2.0f
        var fvalue24 = 1.0f
        var fvalue25 = 0.0f

     //   var workBuffer = ByteArray(80)

    //    var tmpOffset = 0
        val ll = buffer.size

        // FileSave EXCELでグラフ化しやすいように重みづけする
        var io0:Short = 5
        var io1:Short = 4
        var io2:Short = 3
        var io3:Short = 2
        var io4:Short = 1
        var io5:Short = 0

/* -----------------------------------------------------------
   下2ビットが00までのオフセットを求める
   tmpOffsetにいれて、画面上面に表示する
   -----------------------------------------------------------

        for (i in 0..10) {
            // 連続した2バイトの下2ビットが00

            if (((buffer[i] and (0x03)) == 0x00.toByte()) and ((buffer[i + 1] and (0x03)) == 0x00.toByte())) {
                //    offset = 5- (i % 5)
                //    tmpOffset = i

                //   if((tmpOffset == 5) or (tmpOffset == 0)) {
                //       offset = 0
                //   }
                break
            }
            tmpOffset++
        }

        Log.d(TAG, "onDataRead: " + tmpOffset)
        mEdRead!!.setText(tmpOffset.toString())             // オフセットを画面に出す
        ----------------------------------------------------------- */



/* -------------------------------------------------------------------------------
 連続して、00 00 01 01 の並びの時に値を抽出する。
     12bitでマスクする。
-------------------------------------------------------------------------------- */


        for (i in 0..10) {                        // データが10個なら0と1
            // 連続した2バイトの下2ビットが00
            if (((buffer[i] and (0x03)) == 0x00.toByte()) and ((buffer[i+1] and (0x03)) == 0x00.toByte())
                    and ((buffer[i+2] and (0x03)) == 0x01.toByte()) and ((buffer[i+3] and (0x03)) == 0x01.toByte())) {

                var v = buffer[i + 1].toInt() and (0xFC)
                var u = buffer[i].toInt() and (0xFC)
                var nn = u.shl(4) + v.ushr(2)
                //    nn = nn and (0xFFF)                               // ここのマスクはあってもなくてもい
                fvalue1 = nn.toFloat()               // kawa Floatに変換して使う


                v = buffer[i+3].toInt() and (0xFC)
                u = buffer[i+2].toInt() and (0xFC)
                nn = u.shl(4) + v.ushr(2)
                //     nn = nn and (0xFFF)
                fvalue2 = nn.toFloat()               // kawa Floatに変換して使う

                // io0～io5のデフォルト値
                fvalue20 = 5.0f
                fvalue21 = 4.0f
                fvalue22 = 3.0f
                fvalue23 = 2.0f
                fvalue24 = 1.0f
                fvalue25 = 0.0f

                v = buffer[i+4].toInt() and (0xFC)
                nn = v.ushr(2)

                // ビットごとにチェック、1なら線が立つ。保存するデータは0か1の整数
                // EXELでグラフ化しやすいように各ビット0と1ではなく、重みづける
                if ((nn and 0x01) == 0x01) {
                    fvalue20 = 5.8f
                    io0 = 6
                }
                if ((nn and 0x02) == 0x02) {
                    fvalue21 = 4.8f
                    io1 = 5
                }
                if ((nn and 0x04) == 0x04) {
                    fvalue22 = 3.8f
                    io2 = 4
                }
                if ((nn and 0x08) == 0x08) {
                    fvalue23 = 2.8f
                    io3 = 3
                }
                if ((nn and 0x10) == 0x10) {
                    fvalue24 = 1.8f
                    io4 = 2
                }
                if ((nn and 0x20) == 0x20) {
                    fvalue25 = 0.8f
                    io5 = 1
                }

                break
            }
        }



        // 赤のラインで12ビットの範囲の値でなければ、表示しない。これで見かけ上不連続なし
        // ただし、サンプル1回分抜けるので、よく見ると段差が見える

        // 不連続、初期値の時は1回前の値を使い描画は毎サンプル行う
        if (fvalue2 > 4095) {
            fvalue1 = prevf1
            fvalue2 = prevf2



        }

        // ダミーでio1を変化させる
        /*
        if (data2.xValCount %  100 == 0) {
            fvalue22 = 1.3f}
        else {
            fvalue22 = 0.5f
        }
        // ダミーでio3を変化させる
        if (data2.xValCount %  333 == 0) {
            fvalue24 = 3.3f}
        else {
            fvalue24 = 2.5f
        }
         */

// STARTボタンをタップしたら表示する。ボタンの表示がSTOPになっている。
        // STARTボタンを押して測定実施中はSTOP表示になっている。
        if (start_btn.text == "STOP") {
            data.addEntry(Entry(fvalue1, set1.getEntryCount()), 0)
            data.addEntry(Entry(fvalue2, set2.getEntryCount()), 1)
            data2.addEntry(Entry(fvalue20, set20.getEntryCount()), 0)
            data2.addEntry(Entry(fvalue21, set21.getEntryCount()), 1)
            data2.addEntry(Entry(fvalue22, set22.getEntryCount()), 2)
            data2.addEntry(Entry(fvalue23, set23.getEntryCount()), 3)
            data2.addEntry(Entry(fvalue24, set24.getEntryCount()), 4)
            data2.addEntry(Entry(fvalue25, set25.getEntryCount()), 5)
            // 0815      }  // ここで閉じてしまうと、SAVE中にも配列を更新して値が入ってしまう。
            //                 非同期処理にして、マルチタスクにしたのでこの問題が起きた。NORMAL画面は正しかった。　


            //  データを追加したら必ずよばないといけない
            //   data.notifyDataChanged()
            mChart?.notifyDataSetChanged()
            mChart?.setVisibleXRangeMaximum(xLenght)
            mChart?.setVisibleXRangeMinimum(xLenght)           // 最小値を最大値と同じにすると軸が固定
            //      mChart?.moveViewToX(data.xValCount - (xLenght + 1f)) //  移動する

            mChart2?.notifyDataSetChanged()
            mChart2?.setVisibleXRangeMaximum(xLenght)
            mChart2?.setVisibleXRangeMinimum(xLenght)

            // X軸を固定したときの画像の移動方法
            // 軸いっぱいに達するまでは単に更新する。それ以降の時は移動させる
            if (loopCount < xLenght) {
                mChart?.invalidate()               // 更新する
                mChart2?.invalidate()
                loopCount++
            } else {
                mChart?.moveViewToX(data.xValCount - (xLenght + 1f)) //  移動する
                mChart2?.moveViewToX(data2.xValCount - (xLenght + 1f)) //  移動する
                //    loopCount = xLenght
            }
            //   loopCount++                // 描き始めに軸に達するまでをカウントする

// 今回のデータを次回のために保存する
            prevf1 = fvalue1
            prevf2 = fvalue2

            // FileSave
            DataArray[saveOffset][0] = fvalue1.toShort()
            DataArray[saveOffset][1] = fvalue2.toShort()
            DataArray[saveOffset][2] = io0
            DataArray[saveOffset][3] = io1
            DataArray[saveOffset][4] = io2
            DataArray[saveOffset][5] = io3
            DataArray[saveOffset][6] = io4
            DataArray[saveOffset][7] = io5

            Log.d(TAG, "DataArray: " + saveOffset + ": " + fvalue1)

            //    データカウント数、あまりにも大きくなったらバッファサイズまで小さくする
            saveCount = saveCount + 1
            if (saveCount >= 922337203685477580) {
                saveCount = saveBufSize.toLong()
            }

            saveOffset = saveOffset + 1
            if (saveOffset >= saveBufSize) {
                saveOffset = 0
            }
        }       // 0815 表示がSTOP、つまり表示中の時だけ配列更新する

    }   // ================================== end of onDataRead() ===========================





// -----------------------------------------------------------------
// kawa これは作った関数

    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = (bytes[j] and 0xFF.toByte()).toInt()

            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }
// --------------------------------------------------------------------







    override fun onDestroy() {
        super.onDestroy()
        mService!!.disconnect()
    }

    override fun onStatusChange(status: BluetoothStatus) {
        Log.d(TAG, "onStatusChange: $status")
    }

    override fun onDeviceName(deviceName: String) {
        Log.d(TAG, "onDeviceName: $deviceName")
    }

    override fun onToast(message: String) {
        Log.d(TAG, "onToast")
    }

    override fun onDataWrite(buffer: ByteArray) {
        Log.d(TAG, "onDataWrite")
        mEdRead!!.append("> " + String(buffer))
    }

    override fun onClick(v: View) {
        //       mWriter!!.writeln(mEdWrite!!.text.toString())
        //       mEdWrite!!.setText("")
    }

    companion object {
        private const val TAG = "DeviceActivity"
    }
}