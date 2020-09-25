package com.example.proxima

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.activity_new_message.*
import kotlinx.android.synthetic.main.user_row.view.*
import org.json.JSONObject
import java.util.*

class NewMessageActivity : AppCompatActivity() {

    private val FCM_API = $FCM_API
    private val serverKey =$SERVER_KEY
    private val contentType = "application/json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_message)

        supportActionBar?.title = "Select User"

        fetchUsers()
    }


    companion object{
        val USER_KEY = "USER_KEY"
    }
    private fun fetchUsers() {
        val ref = FirebaseDatabase.getInstance().getReference("/users")
                    ref.addListenerForSingleValueEvent(object: ValueEventListener{
                        override fun onDataChange(p0: DataSnapshot) {
                            val adapter = GroupAdapter<GroupieViewHolder>()


                            p0.children.forEach {
                                val user = it.getValue(User::class.java)
                                if (user != null) {
                                    adapter.add(UserItem(user))
                                 }
                }
                adapter.setOnItemClickListener{ item, view ->
                    val userItem = item as UserItem
                    val intent = Intent(view.context, ChatLogActivity::class.java)
                    intent.putExtra(USER_KEY, userItem.user)
                    startActivity(intent)
                    finish()
                }
                newmessage.adapter = adapter
            }
            override fun onCancelled(p0: DatabaseError) {
            }

        })
    }
}

class UserItem(val user: User): Item<GroupieViewHolder>(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.username_textView.text = user.username
    }

    override fun getLayout(): Int {
        return R.layout.user_row
    }
}

