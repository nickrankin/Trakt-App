package com.nickrankin.traktapp.ui.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.nickrankin.traktapp.R
import com.uwetrottmann.trakt5.enums.Rating

class RatingPickerFragment(private val callback: (newRating: Int) -> Unit, var title: String): DialogFragment(), NumberPicker.OnValueChangeListener {
    private val DEFAULT_RATING_VALUE = 8
    private lateinit var ratingsTextHelp: TextView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)

        val numberPickerDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(activity)
        val layoutInflater = context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view: View = layoutInflater.inflate(R.layout.dialog_ratings, null)
        numberPickerDialogBuilder.setView(view)

        // Initialize the Views
        val numberPicker: NumberPicker = view.findViewById(R.id.dialog_ratings_numbpicker)
        val rateButton: Button = view.findViewById(R.id.dialog_ratings_ratebtn)
        val resetButton: Button = view.findViewById(R.id.dialog_ratings_resetbtn)
        val closeButton: Button = view.findViewById(R.id.dialog_ratings_closebtn)
        ratingsTextHelp = view.findViewById(R.id.dialog_ratings_texthelp)

        numberPicker.displayedValues = arrayOf(
            "1",
            "2",
            "3",
            "4",
            "5",
            "6",
            "7",
            "8",
            "9",
            "10"
        )
        numberPicker.minValue = 1
        numberPicker.maxValue = 10

        // TODO Allow the default rating value to be customized
        numberPicker.value = DEFAULT_RATING_VALUE

        ratingsTextHelp.text = "Rate $title with ($DEFAULT_RATING_VALUE) - ${
            Rating.fromValue(
                DEFAULT_RATING_VALUE
            ).name}"

        numberPicker.setOnValueChangedListener(this)

        rateButton.setOnClickListener {
            callback(numberPicker.value)

            dialog?.dismiss()
        }

        resetButton.setOnClickListener {
            callback(-1)
            dialog?.dismiss()
        }

        closeButton.setOnClickListener { dialog?.dismiss() }
        // create the dialog from the builder then show
        return numberPickerDialogBuilder.create()
    }
    override fun onValueChange(picker: NumberPicker?, oldValue: Int, newValue: Int) {
        ratingsTextHelp.text = "Rate $title with ($newValue) - ${
            Rating.fromValue(
                newValue
            ).name}"
    }

    companion object {
        interface OnRatingChangedListener {
            fun onRatingChanged(newRating: Int)
        }
    }
}