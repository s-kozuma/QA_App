package kozuma.shun.techacademy.jp

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.Toolbar
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_question_detail.*
import kotlinx.android.synthetic.main.app_bar_main.*

class FavoriteList : AppCompatActivity()  {

    private lateinit var mToolbar: Toolbar
    private var mGenre = 0

    private lateinit var ff: FloatingActionButton

    private lateinit var mDatabaseReference: DatabaseReference
    private lateinit var mListView: ListView
    private lateinit var mQuestionArrayList: ArrayList<Question>
    private lateinit var mAdapter: QuestionsListAdapter

    private var mGenreRef: DatabaseReference? = null

    var mFavorRef: DatabaseReference? = null


    private val post = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            val map = dataSnapshot.value as Map<String, String>
            val title = map["title"] ?: ""
            val body = map["body"] ?: ""
            val name = map["name"] ?: ""
            val uid = map["uid"] ?: ""
            val imageString = map["image"] ?: ""
            val bytes =
                if (imageString.isNotEmpty()) {
                    Base64.decode(imageString, Base64.DEFAULT)
                } else {
                    byteArrayOf()
                }

            val answerArrayList = ArrayList<Answer>()
            val answerMap = map["answers"] as Map<String, String>?
            if (answerMap != null) {
                for (key in answerMap.keys) {
                    val temp = answerMap[key] as Map<String, String>
                    val answerBody = temp["body"] ?: ""
                    val answerName = temp["name"] ?: ""
                    val answerUid = temp["uid"] ?: ""
                    val answer = Answer(answerBody, answerName, answerUid, key)
                    answerArrayList.add(answer)
                }
            }

            val question = Question(title, body, name, uid, dataSnapshot.key ?: "",
                mGenre, bytes, answerArrayList)
            mQuestionArrayList.add(question)
            mAdapter.notifyDataSetChanged()

        }

        override fun onCancelled(p0: DatabaseError) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }

    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {

            val map = dataSnapshot.value as Map<String, String>

            val fqid = dataSnapshot.key
            val fqgenre = map["genre"]

            println(mDatabaseReference.child(ContentsPATH).child(fqgenre.toString()).child(fqid.toString()))


            mFavorRef = mDatabaseReference.child(ContentsPATH).child(fqgenre.toString()).child(fqid.toString())
            mFavorRef!!.addListenerForSingleValueEvent(post)

        }



        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>

            // 変更があったQuestionを探す
            for (question in mQuestionArrayList) {
                if (dataSnapshot.key.equals(question.questionUid)) {
                    // このアプリで変更がある可能性があるのは回答(Answer)のみ
                    question.answers.clear()
                    val answerMap = map["answers"] as Map<String, String>?
                    if (answerMap != null) {
                        for (key in answerMap.keys) {
                            val temp = answerMap[key] as Map<String, String>
                            val answerBody = temp["body"] ?: ""
                            val answerName = temp["name"] ?: ""
                            val answerUid = temp["uid"] ?: ""
                            val answer = Answer(answerBody, answerName, answerUid, key)
                            question.answers.add(answer)
                        }
                    }

                    mAdapter.notifyDataSetChanged()
                }
            }
        }

        override fun onChildRemoved(p0: DataSnapshot) {

        }

        override fun onChildMoved(p0: DataSnapshot, p1: String?) {

        }

        override fun onCancelled(p0: DatabaseError) {

        }


    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mToolbar = findViewById(R.id.toolbar)

        mToolbar.title = "お気に入り"

        ff = findViewById(R.id.fab)

        ff.hide()

        // Firebase
        mDatabaseReference = FirebaseDatabase.getInstance().reference


        // ListViewの準備
        mListView = findViewById(R.id.listView)
        mAdapter = QuestionsListAdapter(this)
        mQuestionArrayList = ArrayList<Question>()
        mAdapter.notifyDataSetChanged()

        mListView.setOnItemClickListener { parent, view, position, id ->
            // Questionのインスタンスを渡して質問詳細画面を起動する
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", mQuestionArrayList[position])
            startActivity(intent)
        }

        val drawers = findViewById<DrawerLayout>(R.id.drawer_layout)
        drawers.closeDrawer(GravityCompat.START)

        // 質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
        mQuestionArrayList.clear()
        mAdapter.setQuestionArrayList(mQuestionArrayList)
        mListView.adapter = mAdapter

        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        println(mDatabaseReference.child(FavoritesPATH).child(uid))

        mGenreRef = mDatabaseReference.child(FavoritesPATH).child(uid)

        mGenreRef!!.addChildEventListener(mEventListener)
    }




}