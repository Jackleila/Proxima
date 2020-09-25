package com.example.proxima

import android.app.NotificationManager
import android.app.PendingIntent.getActivity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.AuthFailureError
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.activity_chat_log.*
import kotlinx.android.synthetic.main.chat_from_row.view.*
import kotlinx.android.synthetic.main.chat_to_row.view.*
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*


class ChatLogActivity : AppCompatActivity() {
    private val FCM_API = $FCM_API
    private val serverKey = $SERVER_KEY
    private val contentType = "application/json"

    companion object{
        val TAG ="ChatLog"

    }
    val adapter = GroupAdapter<GroupieViewHolder>()


    val toUser:User? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log)

        recyclerview_chat_log.adapter = adapter


        //val username = intent.getStringExtra(NewMessageActivity.USER_KEY)
        val toUser = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        supportActionBar?.title = toUser?.username


        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()

        FirebaseMessaging.getInstance().subscribeToTopic("/topics/" + FirebaseAuth.getInstance().uid)

        listenForMessages()

        val linearLayoutManager = LinearLayoutManager(this);
        linearLayoutManager.stackFromEnd = true
        recyclerview_chat_log.layoutManager= linearLayoutManager;



        button5.setOnClickListener {
            val message_text = editText6.text
            sendMessage()

            val topic = "/topics/" + toUser.uid//topic has to match what the receiver subscribed to
            val notification = JSONObject()
            val notifcationBody = JSONObject()



            try {
                val ref = FirebaseDatabase.getInstance().getReference("/users/")
                val user = FirebaseAuth.getInstance().uid.toString()
                ref.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(p0: DataSnapshot) {
                        val name = p0.child(user).child("username").getValue()
                        notifcationBody.put("title", name)
                        notifcationBody.put(
                            "message", message_text
                        )   
                        notification.put("to", topic)
                        notification.put("data", notifcationBody)


                        sendNotification(notification)
                        editText6.text.clear()
                    }

                    override fun onCancelled(p0: DatabaseError) {
                    }

                })

            } catch (e: JSONException) {
                Log.e("TAG", "onCreate: " + e.message)
            }

        }
    }

    private val requestQueue: RequestQueue by lazy {
        Volley.newRequestQueue(this.applicationContext)
    }

    private fun sendNotification(notification: JSONObject) {
        Log.e("TAG", "sendNotification")
        val jsonObjectRequest = object : JsonObjectRequest(FCM_API, notification,
            Response.Listener<JSONObject> { response ->
                Log.i("TAG", "onResponse: $response")

            },
            Response.ErrorListener {
                Toast.makeText(this@ChatLogActivity, "Request error", Toast.LENGTH_LONG).show()
                Log.i("TAG", "onErrorResponse: Didn't work")
            }) {

            override fun getHeaders(): Map<String, String> {
                val params = HashMap<String, String>()
                params["Authorization"] = serverKey
                params["Content-Type"] = contentType
                return params
            }
        }
        requestQueue.add(jsonObjectRequest)
    }

    private fun listenForMessages() {
        val fromId = FirebaseAuth.getInstance().uid
        val toUser = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        val toId = toUser?.uid

        val ref = FirebaseDatabase.getInstance().getReference("/users-messages/$fromId/$toId")
        ref.addChildEventListener(object: ChildEventListener{

            override fun onChildAdded(p0: DataSnapshot, p1: String?) {

                val chatMessage = p0.getValue(ChatMessage::class.java)
                if(chatMessage!=null){
                    if(chatMessage.fromId == FirebaseAuth.getInstance().uid){
                        adapter.add(ChatToItem(chatMessage.text, convertLongToTime(chatMessage.timestamp)))
                    }else{
                        adapter.add(ChatFromItem(chatMessage.text, convertLongToTime(chatMessage.timestamp)))
                    }
                    Log.d(TAG, "COUNT: "+adapter.itemCount.toString())
                    recyclerview_chat_log.scrollToPosition(adapter.itemCount-1)
                }
            }

            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onChildChanged(p0: DataSnapshot, p1: String?) {
            }

            override fun onChildMoved(p0: DataSnapshot, p1: String?) {
            }

            override fun onChildRemoved(p0: DataSnapshot) {
            }

        })
    }

    class ChatMessage(val id:String, val text: String, val fromId:String, val toId:String, val timestamp:Long){
        constructor():this("","","", "", -1)
    }
    private fun sendMessage() {
        //val reference = FirebaseDatabase.getInstance().getReference("/messages").push()
        val text = editText6.text.toString()
        val fromId = FirebaseAuth.getInstance().uid

        val user = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        val toId = user.uid


        if(fromId==null) return
        val reference = FirebaseDatabase.getInstance().getReference("/users-messages/$fromId/$toId").push()

        val toReference = FirebaseDatabase.getInstance().getReference("/users-messages/$toId/$fromId").push()

        val chatMessage = ChatMessage(reference.key!!, text, fromId, toId, System.currentTimeMillis())
        reference.setValue(chatMessage)
            .addOnSuccessListener {
                Log.d(TAG, "COUNT: "+adapter.itemCount.toString())
                recyclerview_chat_log.scrollToPosition(adapter.itemCount-1)
            }
        toReference.setValue(chatMessage)
            .addOnSuccessListener {
                Log.d(TAG, "message saved")
            }
        val latestMessagesRef = FirebaseDatabase.getInstance().getReference("/latest-messages/$fromId/$toId")
        latestMessagesRef.setValue(chatMessage)

        val latestMessagesToRef = FirebaseDatabase.getInstance().getReference("/latest-messages/$toId/$fromId")
        latestMessagesToRef.setValue(chatMessage)

    }
}
fun convertLongToTime(time: Long): String {
    val date = Date(time)
    val format = SimpleDateFormat("HH:mm")
    val tz = TimeZone.getDefault()
    format.timeZone = TimeZone.getTimeZone(tz.getDisplayName(false, TimeZone.SHORT))
    return format.format(date)
}

class ChatFromItem(val text:String, val date: String): Item<GroupieViewHolder>(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.text_from_row.text = text
        viewHolder.itemView.date_from.text = date
    }

    override fun getLayout(): Int {
        return R.layout.chat_from_row
    }

}


class ChatToItem(val text:String, val date: String): Item<GroupieViewHolder>(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.text_to_row.text = text
        viewHolder.itemView.date_to.text = date
    }

    override fun getLayout(): Int {
        return R.layout.chat_to_row
    }

}
