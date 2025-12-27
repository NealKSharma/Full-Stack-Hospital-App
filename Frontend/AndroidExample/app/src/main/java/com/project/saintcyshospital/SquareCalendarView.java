package com.project.saintcyshospital;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import java.util.Calendar;
import java.util.Locale;

public class SquareCalendarView extends LinearLayout {

    public interface OnDateSelectedListener {
        void onDateSelected(int year, int monthZeroBased, int day);
    }

    private TextView tvMonth;
    private ImageButton btnPrev, btnNext;
    private GridLayout gridDays;

    private Calendar displayMonth;
    private Calendar selectedDate;
    private OnDateSelectedListener listener;

    private AppCompatTextView lastSelectedView;
    private java.util.Set<Integer> markedDays = new java.util.HashSet<>();

    public SquareCalendarView(Context context) {
        super(context);
        init(context);
    }

    public SquareCalendarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SquareCalendarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context ctx) {
        setOrientation(VERTICAL);
        LayoutInflater.from(ctx).inflate(R.layout.view_square_calendar, this, true);

        tvMonth = findViewById(R.id.tvMonth);
        btnPrev = findViewById(R.id.btnPrevMonth);
        btnNext = findViewById(R.id.btnNextMonth);
        gridDays = findViewById(R.id.gridDays);

        displayMonth = Calendar.getInstance();
        displayMonth.set(Calendar.DAY_OF_MONTH, 1);

        selectedDate = Calendar.getInstance();
        selectedDate.set(Calendar.HOUR_OF_DAY, 0);
        selectedDate.set(Calendar.MINUTE, 0);
        selectedDate.set(Calendar.SECOND, 0);
        selectedDate.set(Calendar.MILLISECOND, 0);

        btnPrev.setOnClickListener(v -> {
            displayMonth.add(Calendar.MONTH, -1);
            populateMonth();
        });

        btnNext.setOnClickListener(v -> {
            displayMonth.add(Calendar.MONTH, 1);
            populateMonth();
        });

        populateMonth();
    }

    public void setOnDateSelectedListener(OnDateSelectedListener l) {
        this.listener = l;
        // fire initial selection
        if (selectedDate != null && listener != null) {
            listener.onDateSelected(
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH)
            );
        }
    }

    public long getSelectedDateInMillis() {
        return (selectedDate != null) ? selectedDate.getTimeInMillis()
                : System.currentTimeMillis();
    }

    private void populateMonth() {
        String title = displayMonth.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
                + " " + displayMonth.get(Calendar.YEAR);
        tvMonth.setText(title);

        gridDays.removeAllViews();
        gridDays.setColumnCount(7);

        Calendar cal = (Calendar) displayMonth.clone();
        int firstDayOfWeek = Calendar.SUNDAY;
        int dayOfWeekOfFirst = cal.get(Calendar.DAY_OF_WEEK);
        int leadingBlanks = (dayOfWeekOfFirst - firstDayOfWeek + 7) % 7;

        for (int i = 0; i < leadingBlanks; i++) {
            gridDays.addView(makeEmptyCell());
        }

        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int year = displayMonth.get(Calendar.YEAR);
        int month = displayMonth.get(Calendar.MONTH); // 0-based

        for (int day = 1; day <= daysInMonth; day++) {
            AppCompatTextView tv = makeDayCell(year, month, day);

            if (sameDate(selectedDate, displayMonth, day)) {
                selectCell(tv, day);
            }

            gridDays.addView(tv);
        }
    }


    private boolean sameDate(Calendar selected, Calendar month, int day) {
        if (selected == null) return false;
        return selected.get(Calendar.YEAR) == month.get(Calendar.YEAR)
                && selected.get(Calendar.MONTH) == month.get(Calendar.MONTH)
                && selected.get(Calendar.DAY_OF_MONTH) == day;
    }

    private View makeEmptyCell() {
        AppCompatTextView tv = new AppCompatTextView(getContext());
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = LayoutParams.WRAP_CONTENT;
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(0,0,0,0);
        tv.setLayoutParams(lp);
        return tv;
    }

    private AppCompatTextView makeDayCell(int year, int monthZeroBased, int day) {
        AppCompatTextView tv = new SquareDayTextView(getContext());
        tv.setText(String.valueOf(day));
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(14);
        tv.setBackgroundResource(R.drawable.calendar_day_bg);
        tv.setTextColor(0xFF212121);

        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = LayoutParams.WRAP_CONTENT;
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(0,0,0,0);
        tv.setLayoutParams(lp);

        // mark days that have at least one appointment
        if (markedDays.contains(makeKey(year, monthZeroBased, day))) {
            tv.setActivated(true);  // triggers state_activated in the drawable
        }

        tv.setOnClickListener(v -> selectCell(tv, day));

        return tv;
    }


    private void selectCell(AppCompatTextView tv, int day) {
        if (lastSelectedView != null) {
            lastSelectedView.setSelected(false);
        }
        tv.setSelected(true);
        lastSelectedView = tv;

        if (selectedDate == null) {
            selectedDate = Calendar.getInstance();
        }
        selectedDate.set(Calendar.YEAR, displayMonth.get(Calendar.YEAR));
        selectedDate.set(Calendar.MONTH, displayMonth.get(Calendar.MONTH));
        selectedDate.set(Calendar.DAY_OF_MONTH, day);
        selectedDate.set(Calendar.HOUR_OF_DAY, 0);
        selectedDate.set(Calendar.MINUTE, 0);
        selectedDate.set(Calendar.SECOND, 0);
        selectedDate.set(Calendar.MILLISECOND, 0);

        if (listener != null) {
            listener.onDateSelected(
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH), // 0-based
                    selectedDate.get(Calendar.DAY_OF_MONTH)
            );
        }
    }

    private int makeKey(int year, int monthZeroBased, int day) {
        // monthZeroBased is 0..11; store as 1..12 just for readability
        return year * 10000 + (monthZeroBased + 1) * 100 + day;
    }

    public void setMarkedDays(java.util.Set<Integer> keys) {
        markedDays.clear();
        if (keys != null) markedDays.addAll(keys);
        populateMonth();
    }

    /**
     * TextView whose height == width so each day is a square.
     */
    public static class SquareDayTextView extends AppCompatTextView {
        public SquareDayTextView(Context context) {
            super(context);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, widthMeasureSpec);
        }
    }
}
