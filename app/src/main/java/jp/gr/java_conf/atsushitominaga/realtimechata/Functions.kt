package jp.gr.java_conf.atsushitominaga.realtimechata

import android.content.Context
import android.widget.Toast

fun makeToast(content: Context, message: String){
    Toast.makeText(content,message,Toast.LENGTH_SHORT).show()
}