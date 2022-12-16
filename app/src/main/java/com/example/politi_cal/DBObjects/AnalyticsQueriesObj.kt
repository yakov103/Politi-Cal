package com.example.politi_cal.DBObjects

import android.util.Log
import android.widget.Toast
import com.example.politi_cal.data.queries_Interfaces.AnalyticsQueriesInterface
import com.example.politi_cal.db
import com.example.politi_cal.models.CallBack
import com.example.politi_cal.models.Category
import com.example.politi_cal.models.Celeb
import com.example.politi_cal.models.Company
import com.example.politi_cal.models.VoteOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.logging.Logger

class AnalyticsQueriesObj : AnalyticsQueriesInterface {
    override fun getCelebDistribution(callBack: CallBack<Celeb, Map<String, Int>>) =
        CoroutineScope(Dispatchers.IO).launch {
            val result = db.collection("celebs")
                .whereEqualTo("CelebName", callBack.getInput()!!.Name)
                .get().await()
            if (result.documents.isNotEmpty()) {
                var map = HashMap<String, Int>()
                for (document in result) {
                    var left = Integer.parseInt(document["LeftVotes"].toString())
                    val right = Integer.parseInt(document["RightVotes"].toString())
                    map["Left"] = left
                    map["Right"] = right
                }
                callBack.setOutput(map)
                callBack.Call()
            } else {
                callBack.setOutput(null)
                callBack.Call()
            }
        }

    override fun getCompanyDistribution(callBack: CallBack<Company, Map<String, Double>>) =
        CoroutineScope(Dispatchers.IO).launch {
            val result = db.collection("celebs")
                .whereEqualTo("CompanyID", callBack.getInput()!!.companyID)
                .get().await()
            if (result.documents.isNotEmpty()) {
                var map = HashMap<String, Double>()
                map["Left"] = 0.0
                map["Right"] = 0.0
                for (document in result) {
                    var celeb_left = Integer.parseInt(document["LeftVotes"].toString())
                    val celeb_right = Integer.parseInt(document["RightVotes"].toString())
                    if (celeb_left > celeb_right) {
                        map["Left"] = map["Left"]!! + 1
                    } else if (celeb_left < celeb_right) {
                        map["Right"] = map["Right"]!! + 1
                    } else {
                        map["Left"] = map["Left"]!! + 0.5
                        map["Right"] = map["Right"]!! + 0.5
                    }
                }
                callBack.setOutput(map)
                callBack.Call()
            } else {
                callBack.setOutput(null)
                callBack.Call()
            }
        }

    override fun getCategoryStatistics(callBack: CallBack<Category, Map<String, Double>>) =
        CoroutineScope(Dispatchers.IO).launch {
            val categoryId = callBack.getInput()!!.categoryID
            val companies = db.collection("companies")
                .whereEqualTo("CategoryID", categoryId)
                .get().await()
            if(companies.documents.isNotEmpty()){
                var callback_set = HashSet<CallBack<Company, Map<String, Double>>>()
                for(company in companies){
                    val comp = Company(company.id, categoryId, company["CompanyDesc"].toString())
                    var comp_callback = CallBack<Company, Map<String, Double>>(comp)
                    callback_set.add(comp_callback)
                    getCompanyDistribution(comp_callback)
                }

                /**
                 * In this block we will wait till all the queries will finish, aka all the
                 * callbacks status will be true
                 */

                while(true){
                    var finished = true
                    for(call in callback_set){
                        if(!call.getStatus()){
                            finished = false
                        }
                    }
                    if(finished){
                        break
                    }
                }

                var left_counter = 0.0
                var right_counter = 0.0

                for(call in callback_set){
                    val map = call.getOutput()
                    if(map!!["Left"]!! > map["Right"]!!){
                        left_counter +=1
                    }
                    else if(map!!["Left"]!! < map["Right"]!!){
                        right_counter += 1
                    }
                    else{
                        left_counter += 0.5
                        right_counter += 0.5
                    }
                }
                val left_distribution = left_counter / (left_counter + right_counter)
                val right_distribution = right_counter / (left_counter + right_counter)
                val output = HashMap<String, Double>()
                output["Left"] = left_distribution
                output["Right"] = right_distribution
                callBack.setOutput(output)
                callBack.Call()
            }
            else{
                withContext(Dispatchers.Main){
                    Log.d("CategortAnalyticsError", "Category not found")
                }
            }
        }

    override fun getTotalDistribution(callBack: CallBack<Int, Map<String, Double>>)
    = CoroutineScope(Dispatchers.IO).launch{
        val celebs = db.collection("celebs").get().await()
        if(celebs.documents.isNotEmpty()) {
            val total_size = celebs.size()
            var left_precent = 0.0
            var right_precent = 0.0
            for(celeb in celebs){
                val left = Integer.parseInt(celeb["leftVotes"].toString())
                val right = Integer.parseInt(celeb["rightVotes"].toString())
                if(left > right){
                    left_precent += 1.0
                }
                else if(left < right){
                    right_precent += 1.0
                }
                else{
                    left_precent += 0.5
                    right_precent += 0.5
                }
            }
            val total = left_precent + right_precent
            left_precent = left_precent / total
            left_precent *= 100
            right_precent = right_precent / total
            right_precent *= 100
            val map = hashMapOf("Left" to left_precent, "Right" to right_precent)
            callBack.setOutput(map)
            callBack.Call()
        }
    }

}