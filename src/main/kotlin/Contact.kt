import com.google.gson.annotations.SerializedName

data class Contact(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("messages") val messages: ArrayList<Message> = arrayListOf<Message>()
) {
    fun getInfo(): String {
        return "$name ($phone, email: $email)"
    }
}
