package jp.gr.java_conf.atsushitominaga.realtimechata

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
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
import androidx.core.view.GravityCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.dynamiclinks.ktx.androidParameters
import com.google.firebase.dynamiclinks.ktx.dynamicLink
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.nav_header_main.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    var firebaseAuth: FirebaseAuth? = null
    var firebaseUser: FirebaseUser? = null

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

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode){

        }
    }


}
