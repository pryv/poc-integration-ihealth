package lsi.pryv.epfl.pryvironic.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import lsi.pryv.epfl.pryvironic.R;
import lsi.pryv.epfl.pryvironic.structures.Electrode;

public class MonitoringActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_slider);

        Intent intent = getIntent();
        HashMap<String, Electrode> activeElectrodes = (HashMap) intent.getSerializableExtra("electrodes");

        List<Fragment> fragments = new Vector();
        for(Electrode e: activeElectrodes.values()) {
            fragments.add(new MonitorFragment(e.getName()));
        }

        PagerAdapter pageAdapter = new CustomPagerAdapter(super.getSupportFragmentManager(), fragments);
        ViewPager pager = (ViewPager) super.findViewById(R.id.view_pager);
        pager.setAdapter(pageAdapter);
    }

    private class CustomPagerAdapter extends FragmentPagerAdapter {

        private final List<Fragment> fragments;

        public CustomPagerAdapter(FragmentManager fm, List fragments) {
            super(fm);
            this.fragments = fragments;
        }

        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }
    }

    public class MonitorFragment extends Fragment {
        private String title;

        public MonitorFragment(String title) {
            this.title = title;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.page_item, container, false);
            TextView text = (TextView)v.findViewById(R.id.page_title);
            text.setText(title);
            return v;
        }
    }

}