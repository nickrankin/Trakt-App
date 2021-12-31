package com.nickrankin.traktmanager.ui.dialoguifragments

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset
import java.time.ZoneId
import java.util.*

private const val TAG = "WatchedDatePickerFragme"
class WatchedDatePickerFragment(private val onWatchedDateChanged: (watchedDate: OffsetDateTime) -> Unit): DialogFragment(), DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {
    private lateinit var calendar: Calendar

    private var year: Int = -1
    private var month: Int = -1
    private var dayOfMonth: Int = -1

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)

        calendar = Calendar.getInstance()

        return getDatePickerDialog()
    }

    override fun onDateSet(datePicker: DatePicker?, year: Int, month: Int, day: Int) {
        this.year = year
        this.month = month+1
        this.dayOfMonth = day

        getTimePickerDialog().show()
    }

    override fun onTimeSet(timePicker: TimePicker?, hourOfDay: Int, minute: Int) {
        val dateWatched = OffsetDateTime.of(year, month, dayOfMonth, hourOfDay, minute, 0, 0, OffsetDateTime.now().offset)
        onWatchedDateChanged(dateWatched)
    }

    private fun getDatePickerDialog(): DatePickerDialog {

        // Default date is today
        val year: Int = calendar.get(Calendar.YEAR)
        val month: Int = calendar.get(Calendar.MONTH)
        val day: Int = calendar.get(Calendar.DAY_OF_MONTH)

        return DatePickerDialog(requireContext(), this, year, month, day)
    }

    private fun getTimePickerDialog(): TimePickerDialog {
        // Default time is current time
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        return TimePickerDialog(requireContext(), this, hourOfDay, minute, true)
    }
}