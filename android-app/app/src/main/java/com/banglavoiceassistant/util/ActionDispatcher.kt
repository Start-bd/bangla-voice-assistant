package com.banglavoiceassistant.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.provider.MediaStore
import android.util.Log
import com.banglavoiceassistant.data.AgentIntent
import com.banglavoiceassistant.data.AgentResponse

object ActionDispatcher {
    
    private const val TAG = "ActionDispatcher"
    
    fun dispatch(context: Context, response: AgentResponse): ActionResult {
        val intent = parseIntent(response.intent)
        
        return when (intent) {
            AgentIntent.TRANSLATE_ONLY -> {
                ActionResult.Success("Translated: ${response.englishText}")
            }
            
            AgentIntent.CALL_CONTACT -> {
                response.contactName?.let { name ->
                    callContact(context, name)
                } ?: ActionResult.Error("No contact name provided")
            }
            
            AgentIntent.OPEN_CAMERA -> {
                openCamera(context)
            }
            
            AgentIntent.OPEN_YOUTUBE -> {
                response.searchQuery?.let { query ->
                    openYouTubeSearch(context, query)
                } ?: ActionResult.Error("No search query provided")
            }
            
            AgentIntent.LOVABLE_BUILD -> {
                response.promptForBuilder?.let { prompt ->
                    openLovableBuilder(context, prompt)
                } ?: ActionResult.Error("No builder prompt provided")
            }
            
            AgentIntent.UNKNOWN -> {
                ActionResult.Error("Unknown intent: ${response.intent}")
            }
        }
    }
    
    private fun parseIntent(intentString: String): AgentIntent {
        return try {
            AgentIntent.valueOf(intentString.uppercase())
        } catch (e: IllegalArgumentException) {
            AgentIntent.UNKNOWN
        }
    }
    
    private fun callContact(context: Context, name: String): ActionResult {
        return try {
            // Search for contact by name
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                ),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$name%"),
                null
            )
            
            var phoneNumber: String? = null
            cursor?.use {
                if (it.moveToFirst()) {
                    phoneNumber = it.getString(0)
                }
            }
            
            if (phoneNumber != null) {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                ActionResult.Success("Dialing $name")
            } else {
                // Open dialer with name if contact not found
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                ActionResult.Success("Contact not found. Opening dialer.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling contact: ${e.message}")
            ActionResult.Error("Failed to call contact: ${e.message}")
        }
    }
    
    private fun openCamera(context: Context): ActionResult {
        return try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ActionResult.Success("Opening camera")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening camera: ${e.message}")
            ActionResult.Error("Failed to open camera: ${e.message}")
        }
    }
    
    private fun openYouTubeSearch(context: Context, query: String): ActionResult {
        return try {
            val encodedQuery = Uri.encode(query)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://www.youtube.com/results?search_query=$encodedQuery")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ActionResult.Success("Searching YouTube for: $query")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening YouTube: ${e.message}")
            ActionResult.Error("Failed to open YouTube: ${e.message}")
        }
    }
    
    private fun openLovableBuilder(context: Context, prompt: String): ActionResult {
        return try {
            val encodedPrompt = Uri.encode(prompt)
            val url = "https://lovable.dev/?autosubmit=true#prompt=$encodedPrompt"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ActionResult.Success("Opening Lovable builder with prompt")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening Lovable: ${e.message}")
            ActionResult.Error("Failed to open Lovable: ${e.message}")
        }
    }
    
    sealed class ActionResult {
        data class Success(val message: String) : ActionResult()
        data class Error(val message: String) : ActionResult()
    }
}
