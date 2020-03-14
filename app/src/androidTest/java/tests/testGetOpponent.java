package tests;

import android.support.test.rule.ActivityTestRule;
import android.widget.TextView;

import com.andriod.wuziqi.MainActivity;
import com.andriod.wuziqi.R;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class testGetOpponent {
    @Rule
    public ActivityTestRule<MainActivity> mainActivity = new ActivityTestRule<MainActivity>(MainActivity.class);
    @Test
    public void test1() {
        mainActivity.getActivity().test();
        TextView tv = mainActivity.getActivity().findViewById(R.id.resultArea);
        String n = tv.getText().toString();
        assertEquals(n, "jerry");
    }
}
