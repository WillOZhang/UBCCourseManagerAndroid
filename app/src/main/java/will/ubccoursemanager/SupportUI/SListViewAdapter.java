package will.ubccoursemanager.SupportUI;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v4.widget.SlidingPaneLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import will.ubccoursemanager.R;

/**
 * Created by Will on 2017/6/24.
 */

public class SListViewAdapter extends ArrayAdapter<String> {
    private static final String TAG = "Tag";
    private static final String ERROR = "Error";

    private final Context context;
    private final List<String> values;
    private String[] displayString;
    private String firstLineName;
    private List<String> firstLineTextList;
    private List<String> secondLineTextList;

    private static class ViewHolder {
        TextView textView1;
        TextView textView2;
        ImageView imageView;
    }

    public SListViewAdapter(Context context, List<String> values) {
        super(context, -1, values);

        this.context = context;
        this.values = values;

        if (firstLineTextList == null && secondLineTextList == null) {
            firstLineTextList = new ArrayList<>();
            secondLineTextList = new ArrayList<>();
        }

        updateData(values);
        Log.i(TAG, "Section List adapter has been created");
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            //(LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.fdc_list, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.textView1 = (TextView) convertView.findViewById(R.id.firstLine);
            viewHolder.textView2 = (TextView) convertView.findViewById(R.id.secondLine);
            viewHolder.imageView = (ImageView) convertView.findViewById(R.id.icon);

            convertView.setTag(viewHolder);
        } else
            viewHolder = (ViewHolder) convertView.getTag();

        setDisplayData(viewHolder, position);

        return convertView;
    }

    private void setDisplayData(ViewHolder viewHolder, int position) {
        TextView textView1 = viewHolder.textView1;
        TextView textView2 = viewHolder.textView2;

        try {
            String firstLine = firstLineTextList.get(position);
            textView1.setText(firstLine);
            String secondLine = secondLineTextList.get(position);
            textView2.setText(secondLine);
        } catch (IndexOutOfBoundsException e) {
            textView2.setText("");
        }
    }

    private void updateData(List<String> values) {
        String string = values.get(0);
        displayString = string.split("@");
        if (string.contains("courseInfo")) {
            preHandleS();
            Log.i(TAG, "updated sections info");
        }
    }

    private void preHandleS() {
        reset();
        for (int i = 0; i < values.size(); i++) {
            String string = values.get(i);
            if (string.contains("section!")) {
                try {
                    displayString = string.split("section!")[1].split("@");
                    firstLineName = "Section: " + displayString[0] + "   " + displayString[1];
                    firstLineTextList.add(firstLineName);
                    String secondLineName = "Term: " + displayString[2] + "   " + displayString[3];
                    secondLineTextList.add(secondLineName);
                } catch (IndexOutOfBoundsException e) {
                    Log.i(ERROR, string);
                }
            } else if (string.contains("No section for the selected course")) {
                firstLineName = "No section for the selected course";
                firstLineTextList.add(firstLineName);
            }
//            else if (string.contains("courseInfo")) {
//                firstLineName = string.split("courseInfo")[1].split("description")[0];
//                firstLineTextList.add(firstLineName);
//                String secondLineName = string.split("courseInfo")[1].split("description")[1];
//                if (secondLineName.length() > 1)
//                    secondLineTextList.add(secondLineName);
//                else
//                    secondLineTextList.add("");
//            }
        }
    }

    private void reset() {
        firstLineTextList.clear();
        secondLineTextList.clear();
    }
}
