package net.liedman.whatplane;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

public class PlaneListAdapter extends BaseExpandableListAdapter {
    private Activity owner;
    private String[] groups;
    private String[][] children;
    private float[] bearings;
    private List<CompassView> compassViews = new ArrayList<CompassView>(); 
    private AbsListView.LayoutParams textLayoutParams = new AbsListView.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, 24);

    public PlaneListAdapter(Activity owner, String[] groups, String[][] children, float[] bearings) {
        this.owner = owner;
        this.groups = groups;
        this.children = children;
        this.bearings = bearings;
    }
    
    public Object getChild(int groupPosition, int childPosition) {
        return children[groupPosition][childPosition];
    }

    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    public int getChildrenCount(int groupPosition) {
        return children[groupPosition].length;
    }

    public TextView getGenericView() {
        TextView textView = new TextView(owner);
        textView.setLayoutParams(textLayoutParams);
        textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        textView.setPadding(36, 0, 0, 0);
        return textView;
    }

    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
            View convertView, ViewGroup parent) {
        TextView textView = getGenericView();
        textView.setText(getChild(groupPosition, childPosition).toString());
        return textView;
    }

    public Object getGroup(int groupPosition) {
        return groups[groupPosition];
    }

    public int getGroupCount() {
        return groups.length;
    }

    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
            ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) owner.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.planerow, parent, false);
        }
        
        TextView textView = (TextView) v.findViewById(R.id.infoLabel);
        textView.setText(getGroup(groupPosition).toString());
        CompassView cv = (CompassView) v.findViewById(R.id.compass);
        cv.setDirection(bearings[groupPosition]);
        cv.setMinimumWidth(30);
        cv.setMinimumHeight(30);
        compassViews.add(groupPosition, cv);
        
        return v;
    }

    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public boolean hasStableIds() {
        return true;
    }

    public void setHeading(float f) {
        for (int i = 0; i < bearings.length; i++) {
            CompassView compassView = compassViews.get(i);
            compassView.setDirection(bearings[i] - f);
        }
    }
}