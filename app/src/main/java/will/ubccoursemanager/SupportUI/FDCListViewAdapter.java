package will.ubccoursemanager.SupportUI;

import android.app.Activity;
import android.content.Context;
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
 * Created by Will on 2017/5/27.
 */

public class FDCListViewAdapter extends ArrayAdapter<String> {
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

    public FDCListViewAdapter(Context context, List<String> values) {
        super(context, -1, values);
        this.context = context;
        this.values = values;

        if (firstLineTextList == null && secondLineTextList == null) {
            firstLineTextList = new ArrayList<>();
            secondLineTextList = new ArrayList<>();
        }

        updateData(values);
        Log.i(TAG, "FDC List adapter has been created");
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        updateData(values);
    }

    private void updateData(List<String> values) {
        String string = values.get(0);
        displayString = string.split("@");
        if (string.contains("course#")) {
            preHandleC();
            Log.i(TAG, "updated courses info");
        } else if (displayString.length == 1) {
            preHandleF();
            Log.i(TAG, "updated faculties info");
        } else if (displayString.length > 1 && displayString.length < 4) {
            preHandleD();
            Log.i(TAG, "updated departments info");
        }
    }


    private void preHandleF() {
        reset();
        for (int i = 0; i < values.size(); i++) {
            String string = values.get(i);
            displayString = string.split("@");
            firstLineTextList.add(displayString[0]);
        }
    }

    private void preHandleD() {
        reset();
        for (int i = 0; i < values.size(); i++) {
            String string = values.get(i);
            displayString = string.split("@");
            firstLineTextList.add(displayString[1]);
            secondLineTextList.add(displayString[2]);
        }
    }

    private void preHandleC() {
        reset();
        for (int i = 0; i < values.size(); i++) {
            try {
                String string = values.get(i);
                displayString = string.split("course#")[1].split("@");
                firstLineName = displayString[0];
                if (firstLineName.length() > 30) { // better display
                    firstLineName = displayString[0].substring(0, 30) + "...";
                }
                firstLineTextList.add(firstLineName);
                String secondLineName = displayString[1] + " " + displayString[2];
                secondLineTextList.add(secondLineName);
            } catch (IndexOutOfBoundsException e) {
                Log.i(ERROR, e.getMessage() + values.get(i));
            }
        }
    }

    private void reset() {
        firstLineTextList.clear();
        secondLineTextList.clear();
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

        // change the icon for Windows and iPhone // TODO: add statement in each method accordingly
//        String s = values.get(position);
//        if (s.startsWith("iPhone")) {
//            viewHolder.imageView.setImageResource(R.drawable.no);
//        } else {
//            viewHolder.imageView.setImageResource(R.drawable.ok);
//        }

        viewHolder.imageView.setImageResource(R.drawable.ok);

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
}
