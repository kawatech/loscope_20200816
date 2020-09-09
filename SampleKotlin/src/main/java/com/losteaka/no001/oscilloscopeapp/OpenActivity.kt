package com.losteaka.no001.oscilloscopeapp

//import androidx.appcompat.app.AppCompatActivity
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.android.synthetic.main.activity_device.*
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

// ファイル保存を確認するためのダミーデータ
// FileSave int型の配列を作る
// public val test1:IntArray = intArrayOf(1,2,3,4,5)



class OpenActivity : AppCompatActivity() {
    private val TAG = "OpenExampleActivity"
    private val READ_REQUEST_CODE: Int = 42

    // MPAndroidChart
    private var mChart: LineChart? = null           // kawa2
    private var mChart2: LineChart? = null           // kawa3
    // X軸のラベルの間隔
    private var xIntervalRange = 100

    // Read file
    var DataArray = Array(13125, {arrayOfNulls<Short> (8)})
//    var DataArray = arrayOfNulls<Short>(8000)
    var clmsize = 8
    var rowCount = 0
    private lateinit var mToolbar: Toolbar

    // 非同期
    private var progressDialog: ProgressDialog? = null
    var mHandler: Handler? = null
    var filePass = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_example)
        openImage()


        // 非同期
        //ハンドラを生成
        mHandler = Handler()
     //   val buttonSave = findViewById<Button>(R.id.save_btn)

        //ProgressDialogを生成します。
        progressDialog = ProgressDialog(this)
        progressDialog!!.setMessage("Loading")

        //buttonがクリックされた時の処理を登録します。ボタンでないので不要
     //   makeGraph!! { graphProcess() }


    }
// *************************** onCreate()ここまで *************************************


    // 非同期処理

    /**
     * buttonがクリックされた時の処理 => グラフを表示する処理
     */
    private fun graphProcess() {
        //ProgressDialogを表示します。
        progressDialog!!.show()

        //スレッドを生成して起動します。
        val thread = MyThread()
        thread.start()                  // これを実行すると、アプリ停止する。レイアウトを別にしてOK
    }

    internal inner class MyThread() : Thread() {
        override fun run() {
            //時間のかかる処理実行します。

            // グラフ画面を作ってみるぞ
            // 列のサイズでNORMALかDEBUGかを分岐する

            if (clmsize == 2) {              // NORMALのとき
                mChart = findViewById(R.id.chart) as LineChart
                initChart()

                mChart2 = findViewById(R.id.chart2) as LineChart
                initChart2()
                // ここまでで、グラフの枠が作られる

                displayChart()      // 実際にグラフを描くルーチン NORMAL

            } else {              // DEBUGのとき
                mChart = findViewById(R.id.chart) as LineChart
                initChart3()

                mChart2 = findViewById(R.id.chart2) as LineChart
                initChart4()
                // ここまでで、グラフの枠が作られる

                displayChart2()      // 実際にグラフを描くルーチン DEBUG
            }

            // ------------------------------------------------------------
            //メインスレッドのメッセージキューにメッセージを登録します。
            mHandler!!.post(Runnable
            //run()の中の処理はメインスレッドで動作されます。
            { //画面のtextViewへ"処理が完了しました。"を表示させる。
                //ProgressDialogを消去します。
                progressDialog!!.dismiss()
            })
            // --------------------------------------------------------------


        }                // override fun run() ここまで
    }     // internal inner class MyThread() ここまで
// ***************************************************** 非同期追加ここまで ********************



    private fun openImage() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            //  type = "image/*"
             type = "*/*"                // kawa すべてのファイルを見えるようにする
        }

        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            resultData?.data?.also { uri ->
                Log.i(TAG, "Uri: $uri")
                //    dumpImageMetaData(uri)
                //    showImage(uri)                          // ここでアプリ停止、*.csvファイルをイメージファイルとして開こうとするから
                //    val inputStream: InputStream = context.getContentResolver().openInputStream(uri)      // Kotlinではcontext削除

                val inputStream: InputStream? = getContentResolver().openInputStream(uri)
                val inputStreamReader = InputStreamReader(inputStream)
                val reader = BufferedReader(inputStreamReader)

                filePass = "$uri"

// lineに1行分の文字列が入る。行数分だけ実行する。
            //    var rowCount = 0
           //     var idx = 0
                for(line in reader.lines()) {
                    //   var stmp1 = reader.readLine()

                    // カンマの数でNORMAL(2個）のデータか、DEBUG（7個）のデータかを判別する
                    val b = line.chunked(1).filter { it == ","}         // ","で区切った要素の数
                    //var nn = b.size
                    clmsize = b.size
                    val array = line.split(",").map{it.trim()}

                    for(i in 0..clmsize) {
                        DataArray[rowCount][i] = array[i].toShort()         // 1行分のデータを配列に入れる
                    //    DataArray[idx] = array[i].toShort()
                    //    idx++
                    }
                 //   var count = line
                    rowCount = rowCount + 1
                    Log.d(TAG, rowCount.toString())
                }


/* +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            ここから下は、取り込んだデータをグラフに表示するところ
            画面のレイアウトまでは先にやって、グラフの処理だけを非同期処理、別スレッドにする。
            画面レイアウトを別スレッドに含めると、アプリ停止になる。
  +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ */
                // 画面のレイアウトを変更できるかどうかの確認、できなかった。=> できた。
                // ボタンの操作など、画面特有の操作は画面のレイアウトを変更した後にしないと、アプリ停止になる。
                setContentView(R.layout.activity_device);

                // 不要なボタンは無効にしておく
                save_btn.isEnabled = false
                start_btn.isEnabled = false
                backr_btn.isEnabled = false

                // タイトルをファイル名にする
                mToolbar = findViewById(R.id.toolbar)

                // 後ろから21文字を切り出す
                val str1 = filePass
                val str2 = str1.substring(str1.length - 21)
                mToolbar.title = "RECALL : " + str2                     // タイトル表示

                // グラフ描画処理をこの関数に入れて、これを別スレッドで実行する
                graphProcess()
            }
        }
    }




// ************************ ここから、NORMALの時のグラフ描画 *********************************
    /*  --------------------------------------------------------------
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
        //   mChart?.setPinchZoom(false)
        //   mChart?.isScaleXEnabled = true

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

        xl?.isEnabled = true                   // falseのとき、上のラベルが表示されない

        val leftAxis = mChart2?.getAxisLeft()
        //    leftAxis?.textColor = Color.BLACK
        leftAxis?.textColor = Color.LTGRAY


        // Y軸の幅を明示的に1.0にする
        leftAxis?.setLabelCount(6,true)         // Y軸のラベルを6個
        leftAxis?.setGranularity(1.0f)                           // 間隔は1.0

        leftAxis?.setAxisMaxValue( 5.0f)
        leftAxis?.setAxisMinValue(0.0f)
        leftAxis?.setShowOnlyMinMax(true)

        //     leftAxis?.setStartAtZero(false)
        leftAxis?.setDrawGridLines(true)
        val rightAxis = mChart2?.getAxisRight()
        rightAxis?.isEnabled = false
    }
// -------------------------------------------------------------------------


/* -------------------------------------------------------
DataArray[][]にあるデータを取り出してグラフに描く　NORMAL画面
---------------------------------------------------------- */
private fun displayChart() {
    for (row in 0..rowCount - 1) {
        //  追加描画するデータを追加
        val data = mChart?.getData() ?: return
        val data2 = mChart2?.getData() ?: return

        var set1 = data.getDataSetByIndex(0)
        var set2 = data.getDataSetByIndex(1)       /////
        var set3 = data2.getDataSetByIndex(0)       // data2が下段のフラフ

        if (set1 == null) {
            set1 = LineDataSet(null, "データ1")
            set1.color = Color.YELLOW
            set1.setDrawValues(false)
            set1.setDrawCircles(false)          // データの頂点の丸を描画しない
            data.addDataSet(set1)
        }

        if (set2 == null) {
            set2 = LineDataSet(null, "データ2")
            set2.color = Color.parseColor("#00FF00")            // 鮮やかな緑
            set2.setDrawValues(false)
            set2.setDrawCircles(false)
            data.addDataSet(set2)  /////
        }

        if (set3 == null) {
            set3 = LineDataSet(null, "データ3")        // kawa3
            set3.color = Color.YELLOW
            set3.setDrawValues(false)
            set3.setDrawCircles(false)
            data2.addDataSet(set3)
        }

        // ここでは、グラフの上部に時間を入れる。
        val date = Date()
        val format = SimpleDateFormat("HH:mm:ss")
        data.addXValue(format.format(date))
        data2.addXValue(format.format(date))

        // 初期値を設定しておく
        var fvalue1 = 5000f
        var fvalue2 = 5000f
        var fvalue3 = 4500f

        val xLenght = (rowCount - 1).toFloat()

        // データ配列のデータをグラフに渡していく
        fvalue1 = DataArray[row][0]!!.toFloat()
        fvalue2 = DataArray[row][1]!!.toFloat()
        fvalue3 = DataArray[row][2]!!.toFloat()

        data.addEntry(Entry(fvalue1, set1.getEntryCount()), 0)
        data.addEntry(Entry(fvalue2, set2.getEntryCount()), 1)
        data2.addEntry(Entry(fvalue3, set3.getEntryCount()), 0)

//  データを追加したら必ずよばないといけない
        //   data.notifyDataChanged()
        mChart?.notifyDataSetChanged()
        mChart?.setVisibleXRangeMaximum(xLenght)
        mChart?.setVisibleXRangeMinimum(xLenght)           // 最小値を最大値と同じにすると軸が固定

        mChart2?.notifyDataSetChanged()
        mChart2?.setVisibleXRangeMaximum(xLenght)
        mChart2?.setVisibleXRangeMinimum(xLenght)

        mChart?.invalidate()               // 更新する
        mChart2?.invalidate()

        Log.d(TAG, "displayChart: " + row.toString() + ": " + fvalue1.toString())

    }       // =============== end of for() ===========================
}       // ------------ end of displayChart() ---------------------------------------------
// ************************ ここまでが、NORMALの時のグラフ描画 *********************************


    // ************************ ここから、DEBUGの時のグラフ描画 *********************************
    /*  --------------------------------------------------------------
    チャートの初期設定、MPAndroidChartSampleから流用
    ----------------------------------------------------------------- */
    private fun initChart3() {
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



        //     leftAxis?.setStartAtZero(false)
        leftAxis?.setDrawGridLines(true)
        val rightAxis = mChart?.getAxisRight()
        rightAxis?.isEnabled = false
    }

    // ---------------------------------------------------------
    private fun initChart4() {
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

    /* -------------------------------------------------------
    DataArray[][]にあるデータを取り出してグラフに描く。DEBUG画面
    ---------------------------------------------------------- */
    private fun displayChart2() {
        for (row in 0..rowCount - 1) {
            //  追加描画するデータを追加
            val data = mChart?.getData() ?: return
            val data2 = mChart2?.getData() ?: return

            var set1 = data.getDataSetByIndex(0)
            var set2 = data.getDataSetByIndex(1)       /////

            // 下のグラフに描く、dsta2でインデックスは0から
            var set20 = data2.getDataSetByIndex(0)
            var set21 = data2.getDataSetByIndex(1)
            var set22 = data2.getDataSetByIndex(2)
            var set23 = data2.getDataSetByIndex(3)
            var set24 = data2.getDataSetByIndex(4)
            var set25 = data2.getDataSetByIndex(5)

            if (set1 == null) {
                set1 = LineDataSet(null, "データ1")
                set1.color = Color.YELLOW
                set1.setDrawValues(false)
                set1.setDrawCircles(false)          // データの頂点の丸を描画しない
                data.addDataSet(set1)
            }

            if (set2 == null) {
                set2 = LineDataSet(null, "データ2")
                set2.color = Color.parseColor("#00FF00")            // 鮮やかな緑
                set2.setDrawValues(false)
                set2.setDrawCircles(false)
                data.addDataSet(set2)  /////
            }

         /* -------------------------------------------------------------------
        IO0からIO5を準備する
        ---------------------------------------------------------------------- */
            if (set20 == null) {
                set20 = LineDataSet(null, "IO0")        // kawa3
                set20.color = Color.YELLOW
                set20.setDrawValues(false)
                set20.setDrawCircles(false)
                data2.addDataSet(set20)
            }

            if (set21 == null) {
                set21 = LineDataSet(null, "IO1")        // kawa3
                set21.color = Color.parseColor("#ffa500")    //オレンジ
                set21.setDrawValues(false)
                set21.setDrawCircles(false)
                data2.addDataSet(set21)
            }

            if (set22 == null) {
                set22 = LineDataSet(null, "IO2")        // kawa3
                set22.color = Color.MAGENTA                  // 桃色
                set22.setDrawValues(false)
                set22.setDrawCircles(false)
                data2.addDataSet(set22)
            }


            if (set23 == null) {
                set23 = LineDataSet(null, "IO3")        // kawa3
                set23.color = Color.CYAN                     // 空色 //
                set23.setDrawCircles(false)
                data2.addDataSet(set23)
            }


            if (set24 == null) {
                set24 = LineDataSet(null, "IO4")        // kawa3
                set24.color = Color.RED                     // 赤
                set24.setDrawCircles(false)
                data2.addDataSet(set24)
            }


            if (set25 == null) {
                set25 = LineDataSet(null, "IO5")        // kawa3
                set25.color = Color.GREEN                      // 緑
                set25.setDrawCircles(false)
                data2.addDataSet(set25)
            }


            // ここでは、グラフの上部に時間を入れる。やめる。今の時間ではない。
            // ここをマスクすると、データが表示されない。今は残しておく。
            /*----------------------------------*/
            val date = Date()
            val format = SimpleDateFormat("HH:mm:ss")
            data.addXValue(format.format(date))
            data2.addXValue(format.format(date))
/*----------------------------------------------------------------- */
            // 初期値を設定しておく
            var fvalue1 = 5000f
            var fvalue2 = 5000f

            var fvalue20 = 5.0f
            var fvalue21 = 4.0f
            var fvalue22 = 3.0f
            var fvalue23 = 2.0f
            var fvalue24 = 1.0f
            var fvalue25 = 0.0f

            val xLenght = (rowCount - 1).toFloat()

            // データ配列のデータをグラフに渡していく

            fvalue1 = DataArray[row][0]!!.toFloat()
            fvalue2 = DataArray[row][1]!!.toFloat()

            // データが1の時、オフセット付きの0.8にする
            if(DataArray[row][2]!!.toInt() == 6) fvalue20 = 5.8f
            if(DataArray[row][3]!!.toInt() == 5) fvalue21 = 4.8f
            if(DataArray[row][4]!!.toInt() == 4) fvalue22 = 3.8f
            if(DataArray[row][5]!!.toInt() == 3) fvalue23 = 2.8f
            if(DataArray[row][6]!!.toInt() == 2) fvalue24 = 1.8f
            if(DataArray[row][7]!!.toInt() == 1) fvalue25 = 0.8f

            data.addEntry(Entry(fvalue1, set1.getEntryCount()), 0)
            data.addEntry(Entry(fvalue2, set2.getEntryCount()), 1)
            data2.addEntry(Entry(fvalue20, set20.getEntryCount()), 0)
            data2.addEntry(Entry(fvalue21, set21.getEntryCount()), 1)
            data2.addEntry(Entry(fvalue22, set22.getEntryCount()), 2)
            data2.addEntry(Entry(fvalue23, set23.getEntryCount()), 3)
            data2.addEntry(Entry(fvalue24, set24.getEntryCount()), 4)
            data2.addEntry(Entry(fvalue25, set25.getEntryCount()), 5)

//  データを追加したら必ずよばないといけない
            //   data.notifyDataChanged()
            mChart?.notifyDataSetChanged()
            mChart?.setVisibleXRangeMaximum(xLenght)
            mChart?.setVisibleXRangeMinimum(xLenght)           // 最小値を最大値と同じにすると軸が固定

            mChart2?.notifyDataSetChanged()
            mChart2?.setVisibleXRangeMaximum(xLenght)
            mChart2?.setVisibleXRangeMinimum(xLenght)

            mChart?.invalidate()               // 更新する
            mChart2?.invalidate()

            Log.d(TAG, "displayChart2: " + row.toString() + ": " + fvalue1.toString())

        }       // =============== end of for() ===========================
    }       // ------------ end of displayChart2() ---------------------------------------------









    // こちらは使わないのでマスクしておく
/*
    fun dumpImageMetaData(uri: Uri) {
        val cursor: Cursor? = contentResolver.query( uri, null, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val displayName: String =
                        it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                Log.i(TAG, "Display Name: $displayName")

                val sizeIndex: Int = it.getColumnIndex(OpenableColumns.SIZE)
                val size: String = if (!it.isNull(sizeIndex)) {
                    it.getString(sizeIndex)
                } else {
                    "Unknown"
                }
                Log.i(TAG, "Size: $size")
            }
        }
    }

    @Throws(IOException::class)
    private fun getBitmapFromUri(uri: Uri): Bitmap {
        val parcelFileDescriptor: ParcelFileDescriptor? = contentResolver.openFileDescriptor(uri, "r")
        val fileDescriptor: FileDescriptor = parcelFileDescriptor!!.fileDescriptor
        val image: Bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor)
        parcelFileDescriptor.close()
        return image
    }

    private fun showImage(uri: Uri) {
        GlobalScope.launch {
            val bitmap = getBitmapFromUri(uri)
            withContext(Dispatchers.Main) {
                mainImageView.setImageBitmap(bitmap)
            }
        }
    }
*/
}

