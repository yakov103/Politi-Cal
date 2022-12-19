package com.example.politi_cal

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.politi_cal.models.*
import com.example.politi_cal.ui.theme.PolitiCalTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await


val db = Firebase.firestore
val userCollectionRef = Firebase.firestore.collection("users")
val celebCollectionRef = Firebase.firestore.collection("celebs")
val companyCollectionRef = Firebase.firestore.collection("companies")
val categoriesCollectionRef = Firebase.firestore.collection("categories")

var companiesForAddCeleb = mutableListOf<Company>()
var companiesForAddCelebNames = mutableListOf<String>()

var categoriesForAddCeleb = mutableListOf<Category>()
var categoriesForAddCelebNames = mutableListOf<String>()

var celebListParam = mutableListOf<Celeb>()

val userVotesCollectionRef = Firebase.firestore.collection("userVotes")

val celebListFilterNames = mutableListOf<String>()

val checkTrue = DontCotinueUntillTrue()

var CelebForCelebProfile = Celeb(
    "", "", "", 0, "",
    "", "", 0, 0
)


class MainActivity : ComponentActivity() {


    companion object {
        val TAG: String = MainActivity::class.java.simpleName
    }

    private val auth by lazy {
        FirebaseAuth.getInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PolitiCalTheme {
                Navigation(auth = auth)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // use the checkLoggedInState function to check if the user is logged in
        val value = checkLoggedInState(auth)
        //getCelebrities()
        if (value) {
            setContent {
                Navigation(auth = auth, Screen.SwipeScreen.route)
            }
        } else {
            setContent {
                Navigation(auth = auth)
            }

        }
    }

}


fun retrieveUserVotes(callback: CallBack<Boolean, Boolean>) =
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser?.email.toString()
            userVotesCollectionRef.whereEqualTo("userEmail", user).get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val celebName = document.getString("celebFullName")
                        celebListFilterNames.add(celebName.toString())
                    }
                }
            println(celebListFilterNames)
            callback.setOutput(true)
            callback.Call()
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Log.d("Error", "Error: ${e.message}")
            }
        }
    }


fun retrieveCelebs(callBack: CallBack<Boolean, Boolean>) = CoroutineScope(Dispatchers.IO).launch {
    var retries = 0
    var MAX_RETRIES = 5
    var RETRY_DELAY = 1000L
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser?.email.toString()
    val userPref = mutableListOf<String>()
    try {
        val docRef = userCollectionRef.document(user)
        val doc = docRef.get().await()
        for (document in doc.data?.entries!!) {
            if (document.key == "userPref") {
                userPref.addAll(document.value as List<String>)
            }
        }
        println(userPref)
    } catch (e: Exception) {
        Log.e("retrieveCelebs", "Error: ${e.message}")
    }
    val userPrefList = userPref.toList()



    while (true) {
        try {
            val querySnapshot = celebCollectionRef.get().await()
            for (document in querySnapshot.documents) {
                val celebDocument = document
                val celebID = celebDocument.id
                val celebCompany = celebDocument.data?.get("company").toString()
                val celebFirstName = celebDocument.data?.get("firstName").toString()
                val celebLastName = celebDocument.data?.get("lastName").toString()
                val fullName = "$celebFirstName $celebLastName"
                if (celebListFilterNames.contains(fullName)) {
                    continue
                }
                val celebBirthDate = celebDocument.data?.get("birthDate").toString().toLong()
                val celebImgUrl = celebDocument.data?.get("imgUrl").toString()
                val celebInfo = celebDocument.data?.get("celebInfo").toString()
                val celebCategory = celebDocument.data?.get("category").toString()
                if (!userPrefList.contains(celebCategory)) {
                    continue
                }
                val celebRightVotes = celebDocument.data?.get("rightVotes").toString().toLong()
                val celebLeftVotes = celebDocument.data?.get("leftVotes").toString().toLong()
                val celebObject = Celeb(
                    Company = celebCompany,
                    FirstName = celebFirstName,
                    LastName = celebLastName,
                    BirthDate = celebBirthDate,
                    ImgUrl = celebImgUrl,
                    CelebInfo = celebInfo,
                    Category = celebCategory,
                    RightVotes = celebRightVotes,
                    LeftVotes = celebLeftVotes
                )
                celebListParam.add(celebObject)
            }
            celebListParam.shuffle()
            callBack.setOutput(true)
            callBack.Call()
            break

        } catch (e: Exception) {
            if (retries == MAX_RETRIES) {  // If the maximum number of retries is reached
                throw e  // Re-throw the exception to be handled by the caller
            }
            Log.d("error celeb", e.toString())
        }
        retries++  // Increment the retry count
        delay(RETRY_DELAY)  // Wait before retrying
    }
}


// write a function that check if user is logged in take in auth as a parameter and return a boolean
// if user is logged in return true else return false
fun checkLoggedInState(auth: FirebaseAuth): Boolean {
    if (auth.currentUser != null) {
        Log.d(MainActivity.TAG, "User is logged in")
        return true
    } else {
        Log.d(MainActivity.TAG, "User is not logged in")
        return false
    }
}


fun retrieveCompanies() = CoroutineScope(Dispatchers.IO).launch {
    try {
        val querySnapshot = companyCollectionRef.get().await()
        for (document in querySnapshot.documents) {
            val companyDocument = document
            val companyID = companyDocument.id
            val companyCategory = companyDocument.data?.get("category").toString()
            val companyObject = Company(companyID, companyCategory)
            companiesForAddCeleb.add(companyObject)
            companiesForAddCelebNames.add(companyID)
        }
        withContext(Dispatchers.Main) {

        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {

        }
    }
}

fun retrieveCategories() = CoroutineScope(Dispatchers.IO).launch {
    try {
        val querySnapshot = categoriesCollectionRef.get().await()
        for (document in querySnapshot.documents) {
            val categoryDocument = document
            val categoryID = categoryDocument.id
            val categoryName = categoryDocument.data?.get("categoryName").toString()
            val categoryObject = Category(categoryID, categoryName)
            categoriesForAddCeleb.add(categoryObject)
            categoriesForAddCelebNames.add(categoryName)
        }
        withContext(Dispatchers.Main) {

        }
        Log.d("Categories", categoriesForAddCeleb.toString())
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {

        }
        Log.d("Categories", "Error")
    }
}

fun removeCharacters(list: List<String>): List<String> {
    return list.map { it.replace(" ", "").replace("[", "").replace("]", "") }
}


fun retrieveCelebsByUserOfri(callBack: CallBack<Boolean, MutableList<Celeb>>) =
    CoroutineScope(Dispatchers.IO).launch {
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.email.toString()
        val uservotes = db.collection("userVotes")
            .whereEqualTo("userEmail", userId)
            .get().await()
        val userData = db.collection("users").document(userId).get().await()

        var prefs = HashSet<String>()
        if (userData.data != null){
            for (document in userData.data?.entries!!) {
                if (document.key == "userPref") {
                    prefs.addAll(document.value as List<String>)
                }
            }
        }

        var voted_celebs = HashSet<String>()
        if (uservotes.documents.isNotEmpty()) {
            for (document in uservotes) {
                val celebId = document["celebFullName"].toString()
                voted_celebs.add(celebId)
            }
        }
        val celebs = db.collection("celebs")
            .get().await()
        if (celebs.documents.isNotEmpty()) {
            val unvoted_celeb = ArrayList<Celeb>()
            for (celeb in celebs) {
                if (celeb.id in voted_celebs) {
                    continue
                }
                val category = celeb["category"].toString()
                if (!(category in prefs)) {
                    continue
                }
                val fname = celeb["firstName"].toString()
                val lname = celeb["lastName"].toString()
                val birthdate = Integer.parseInt(celeb["birthDate"].toString())
                val company = celeb["company"].toString()
                val imgUrl = celeb["imgUrl"].toString()
                val left = Integer.parseInt(celeb["leftVotes"].toString())
                val right = Integer.parseInt(celeb["rightVotes"].toString())
                val info = celeb["celebInfo"].toString()
                var celeb = Celeb(
                    Company = company,
                    FirstName = fname,
                    LastName = lname,
                    BirthDate = birthdate.toLong(),
                    ImgUrl = imgUrl,
                    CelebInfo = info,
                    RightVotes = right.toLong(),
                    LeftVotes = left.toLong(),
                    Category = category
                )
                unvoted_celeb.add(celeb)
            }
            callBack.setOutput(unvoted_celeb)
            callBack.Call()
        }
    }

