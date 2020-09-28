package com.example.foodmenu

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodmenu.adapter.FoodListAdapter
import com.example.foodmenu.roomDatabase.FoodEntity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.popup_dialog.view.*
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    private lateinit var adapter : FoodListAdapter
    private lateinit var foods : ArrayList<FoodEntity>
    val WRITE_EXTERNAL_STORAGE_CODE = 1
    lateinit var myPassword : String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupTaskRecyclerView()
        title = "Board Menu"
        LoadAllTasksFromDatabase()

        floatingActionButton.setOnClickListener {
            addNewItem()
        }

    }
    private fun addNewItem(){
        val dialogBuilder : AlertDialog.Builder?
        val dialog : AlertDialog?

        val view = LayoutInflater.from(this).inflate(R.layout.popup_dialog, null, false)
        val popEditFood = view?.popEditFood
        val popEditPrice = view?.popEditPrice
        val saveButton = view.editButton

        dialogBuilder = AlertDialog.Builder(this).setView(view)
        dialog = dialogBuilder?.create()
        dialog?.show()

        saveButton.setOnClickListener {
            if (popEditFood?.text.toString().isEmpty() || popEditPrice?.text.toString().isEmpty()){
                Toast.makeText(this, "لطفا نام و قیمت غذا را وارد کنید.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var food = FoodEntity(foodName = popEditFood?.text.toString(), foodPrice = popEditPrice?.text.toString().toDouble() )

            (application as MyApplication).foodDatabase.foodDao().insert(food)
            val foods = (application as MyApplication).foodDatabase.foodDao().getAll()
            adapter.updateList(foods as ArrayList<FoodEntity>)
            Toast.makeText(this, "با موفقیت اضافه شد", Toast.LENGTH_LONG).show()
            dialog?.dismiss()
        }

    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.send_to_board , menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.archive_menu ){
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
                        var permissions = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        requestPermissions(permissions , WRITE_EXTERNAL_STORAGE_CODE)
                    }else{
                        saveToFile()
                    }
                }
                else{
                    saveToFile()
                }


        }
        if(item?.itemId == R.id.info_menu){
            startActivity(Intent(this , ContactActivity::class.java))
            Toast.makeText(this , "More Info" , Toast.LENGTH_LONG).show()
        }
        return super.onOptionsItemSelected(item)
    }


    private fun setupTaskRecyclerView(){
        adapter = FoodListAdapter(this , application as MyApplication)
        myRecyclerView.layoutManager = LinearLayoutManager(this)
        myRecyclerView.adapter = this.adapter

    }

    fun LoadAllTasksFromDatabase(){
            foods = (application as MyApplication).foodDatabase.foodDao().getAll() as ArrayList
            adapter.updateList(foods)


    }

    private fun scrollToBottomOfList(){
        myRecyclerView.smoothScrollToPosition(adapter.itemCount-1)
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode){
            WRITE_EXTERNAL_STORAGE_CODE ->{
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    saveToFile()
                }else{
                    Toast.makeText(this , "نیاز به صدور اجازه دسترسی به حافظه", Toast.LENGTH_LONG).show()
                }

            }
        }
    }

    private fun saveToFile(){
        var timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss" ,
            Locale.getDefault()).format(System.currentTimeMillis())
        var myTime = SimpleDateFormat("yyyy:MM:dd:HH:mm:ss" ,
            Locale.getDefault()).format(System.currentTimeMillis())

        if (passWordEditText.text.isNotEmpty()){
            if(passWordEditText.text.length == 8){
                myPassword = passWordEditText.text.toString()
            }else{
                passWordEditText.text.clear()
                Toast.makeText(this , "لطفا یک پسورد 8 رقمی وارد کنید", Toast.LENGTH_LONG).show()
            }

        }else{
            myPassword = "00000000"
        }

        try {
            val path : File = Environment.getExternalStorageDirectory()
            val dir = File( "$path/My Files/")
            dir.mkdirs()
            val fileName = "MyFile_$timeStamp.txt"
            val file = File(dir , fileName)
            val fileWriter : FileWriter = FileWriter(file.absoluteFile)
            val bufferedWriter = BufferedWriter(fileWriter)
            bufferedWriter.write("@${myPassword}_")
            bufferedWriter.write("${myTime}_${foods.size}_")
            for (i in 0 until foods.size-1){
                bufferedWriter.write("${foods[i].foodPrice}:")
            }
            bufferedWriter.write("${foods.last().foodPrice}@")
            bufferedWriter.close()
            Toast.makeText(this , "$fileName is saved to \n $dir", Toast.LENGTH_LONG).show()
            val data = FileProvider.getUriForFile(this, "com.example.foodmenu", file)
            this.grantUriPermission(this.getPackageName(), data, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val intent = Intent(Intent.ACTION_SEND)
                .setDataAndType(data, "text/*")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
            startActivity(Intent.createChooser(intent, "Share text using"))

        }catch (e : Exception){
            Toast.makeText(this , "${e.message}", Toast.LENGTH_LONG).show()

        }
    }

}