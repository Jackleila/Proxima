package com.example.proxima

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.latest_messages_row.view.*

class LatestMessagesRow(val chatMessage: ChatLogActivity.ChatMessage) : Item<GroupieViewHolder>(){

    var chatPartUser: User? = null

    override fun getLayout(): Int {
        return R.layout.latest_messages_row
    }

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.latest_message_textview.text = chatMessage.text
        val chatPartId:String
        if(chatMessage.fromId== FirebaseAuth.getInstance().uid){
            chatPartId = chatMessage.toId
        }else{
            chatPartId = chatMessage.fromId
        }

        val ref = FirebaseDatabase.getInstance().getReference("/users/$chatPartId")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
                chatPartUser = p0.getValue(User::class.java)
                viewHolder.itemView.user_textview.text =chatPartUser?.username
            }

            override fun onCancelled(p0: DatabaseError) {
            }

        })

    }

}