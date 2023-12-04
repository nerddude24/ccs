import com.google.gson.annotations.SerializedName

data class Message(
    @SerializedName("text") val text: String,
    @SerializedName("recipient") val recipient: String,
    // this is not really used for now, but it's good to save it just in case.
    @SerializedName("id") val id: Int
) {
    fun getDetails(): String {
        return "Contact Name: $recipient \n   Message: '$text'"
    }
}
