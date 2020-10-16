package jp.gr.java_conf.atsushitominaga.realtimechata

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.dynamiclinks.ktx.androidParameters
import com.google.firebase.dynamiclinks.ktx.dynamicLink
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.chat_content.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.nav_header_main.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    var firebaseAuth: FirebaseAuth? = null
    var firebaseUser: FirebaseUser? = null
    var firebaseReference : DatabaseReference? = null
    var layoutManager: LinearLayoutManager? = null
    lateinit var firebaseAdapter: FirebaseRecyclerAdapter<MessageModel, MessageHolder>


    var userName: String = ""
    var userPhotoUrl: String = ""

    lateinit var mGoogleSignInClient: GoogleSignInClient



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)



        val toggle = ActionBarDrawerToggle(
            this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        loginCheck()

        firebaseReference = FirebaseDatabase.getInstance().reference
        displayChatData()

        btnSend.setOnClickListener {
            postMessage()
        }

        btnAddPhoto.setOnClickListener {
            postImage()
        }


        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    override fun onResume() {
        super.onResume()
        firebaseAdapter.startListening()
    }

    override fun onPause() {
        super.onPause()
        firebaseAdapter.stopListening()
    }

    private fun displayChatData() {
        layoutManager = LinearLayoutManager(this)
        layoutManager!!.stackFromEnd = true
        chatList.layoutManager = layoutManager

        val query = firebaseReference!!.child(MY_CHAT_TBL).limitToLast(50)

        val options = FirebaseRecyclerOptions.Builder<MessageModel>()
            .setQuery(query, MessageModel::class.java)
            .build()

        firebaseAdapter = object : FirebaseRecyclerAdapter<MessageModel, MessageHolder>(options){
            override fun onCreateViewHolder(p0: ViewGroup, p1: Int): MessageHolder {
                val view = LayoutInflater.from(p0!!.context).inflate(R.layout.chat_content, p0, false)
                return MessageHolder(view)

            }
            override fun onBindViewHolder(holder: MessageHolder, position: Int, model: MessageModel) {
                setUserContents(holder,model)
                setChatContents(holder,model)

            }
        }
        chatList.adapter = firebaseAdapter

        firebaseAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver(){
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                val chatMessageCont = firebaseAdapter.itemCount
                val lastVisiblePosition = layoutManager!!.findLastCompletelyVisibleItemPosition()

                if (lastVisiblePosition == -1 || positionStart > chatMessageCont -1 && lastVisiblePosition == positionStart -1){
                    chatList.scrollToPosition(positionStart)
                }

                firebaseReference!!.child(MY_CHAT_TBL).addChildEventListener(object : ChildEventListener {
                    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {

                        val newMessage = snapshot!!.getValue(MessageModel::class.java)
                        if (newMessage!!.userName != userName) sendNotification(newMessage)
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }

                    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

                    }

                    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {

                    }



                    override fun onChildRemoved(snapshot: DataSnapshot) {

                    }

                })
            }
        })

    }

    private fun sendNotification(newMessage: MessageModel) {


    }

    private fun setUserContents(holder: MessageHolder, model: MessageModel) {
        holder.text_user_name.text = model.userName
        if(model.userPhotoUrl != ""){
            Glide.with(this)
                .load(Uri.parse(model.userPhotoUrl))
                .into(holder.image_user)

            return
        }
        holder.image_user.setImageDrawable(ContextCompat.getDrawable(this@MainActivity,R.drawable.ic_account_circle_black_24dp))
    }

    private fun setChatContents(holder: MessageHolder, model: MessageModel) {
        if (model.postedMessage != ""){
            holder.apply {
                text_posted.text =model.postedMessage
                text_posted.visibility = View.VISIBLE
                image_posted.visibility = View.GONE

            }
            return
        }
        setImageContent(holder,model)

    }

    private fun setImageContent(holder: MessageHolder, model: MessageModel) {
        val imageUrl = model.postedImageUrl
        holder.apply{
            text_posted.visibility = View.INVISIBLE
            image_posted.visibility = View.VISIBLE
        }
        if(!imageUrl.startsWith("gs//")){
            Glide.with(holder.image_posted.context)
                .load(model.postedImageUrl)
                .into(holder.image_posted)
            return
        }

        val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
        storageRef.downloadUrl.addOnCompleteListener { task ->
            if (!task.isSuccessful){
                makeToast(this@MainActivity, getString(R.string.connection_failed))
                return@addOnCompleteListener
            }
            val downloadUrl = task.result
            Glide.with(this)
                .load(downloadUrl)
                .into(holder.image_posted)
        }



    }


    private fun postMessage() {
        val model = MessageModel(userName, userPhotoUrl, inputMessage.text.toString(), "")
        firebaseReference!!.child(MY_CHAT_TBL).push().setValue(model)
        inputMessage.setText("")
    }

    private fun loginCheck() {

        firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser == null){
            startActivity(Intent(this@MainActivity,SignInActivity::class.java))
            finish()
            return
        }
        // ログインしている場合
        setUserProfiles(firebaseUser!!)



    }

    private fun setUserProfiles(firebaseUser: FirebaseUser) {

        val nav_header = nav_view.getHeaderView(0)
        val textUserName = nav_header.findViewById<TextView>(R.id.text_user_name)
        val textUserEmail = nav_header.findViewById<TextView>(R.id.text_user_id)
        textUserName.text = firebaseUser.displayName
        textUserEmail.text = firebaseUser.email

        userName = firebaseUser.displayName!!
        userPhotoUrl = firebaseUser.photoUrl.toString()


    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }


    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.nav_menu_invite ->{
                sendInvitation()

            }

            R.id.nav_menu_sign_out ->{
                signOut()

            }
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun signOut() {
        FirebaseAuth.getInstance().signOut()

        mGoogleSignInClient.signOut().addOnCompleteListener { task ->
            if(task.isSuccessful){
                startActivity(Intent(this@MainActivity, SignInActivity::class.java))
                finish()
                return@addOnCompleteListener
            }
        }


    }

    private fun sendInvitation() {
        val dynamicLink = Firebase.dynamicLinks.dynamicLink {
            link = Uri.parse("https://realtimechata.page.link/main_activity")
            domainUriPrefix = "https://realtimechata.page.link"
            // Open links with this app on Android
            androidParameters("jp.gr.java_conf.atsushitominaga.realtimechata") {
                fallbackUrl = Uri.parse("https://play.google.com/store/apps/details?id=jp.gr.java_conf.atsushitominaga")
            }

        }

        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT,getString(R.string.app_invite_message) + " " + dynamicLink.uri.toString())
            type = "text/plain"
        }
        startActivity(Intent.createChooser(intent,getString(R.string.app_invite_title)))

    }

    private fun postImage() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        startActivityForResult(intent,REQUEST_GET_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode){
            REQUEST_GET_IMAGE -> getImageResult(resultCode,data)

        }
    }

    private fun getImageResult(resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK){
            makeToast(this@MainActivity, getString(R.string.get_image_failed))
            return
        }

        if(data == null) return
        val uriFromDevice = data.data

        val tempMessage = MessageModel(userName, userPhotoUrl,"","")
        firebaseReference!!.child(MY_CHAT_TBL).push().setValue(tempMessage){ databaseError, databaseReference ->
            if(databaseError != null){
                makeToast(this@MainActivity, getString(R.string.db_write_error))
                return@setValue
            }
            val key = databaseReference.key
            val storageRef = FirebaseStorage.getInstance().getReference(firebaseUser!!.uid).child(key!!)
                .child(uriFromDevice!!.lastPathSegment!!)
            putImageStorage(storageRef,uriFromDevice,key)

        }

    }

    private fun putImageStorage(storageRef: StorageReference, uriFromDevice: Uri, key: String) {
        storageRef.putFile(uriFromDevice!!).continueWithTask { task ->
            if (!task.isSuccessful){ }
            return@continueWithTask storageRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (!task.isSuccessful){
                makeToast(this@MainActivity,getString(R.string.image_upload_error))
                return@addOnCompleteListener
            }
            val chatMessage = MessageModel(userName, userPhotoUrl,"",task.result.toString())
            firebaseReference!!.child(MY_CHAT_TBL).child(key!!).setValue(chatMessage)
        }
    }


}
