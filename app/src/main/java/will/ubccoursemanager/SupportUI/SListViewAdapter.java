package will.ubccoursemanager.SupportUI;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.widget.ArrayAdapter;

import java.util.List;

/**
 * Created by Will on 2017/6/24.
 */

public class SListViewAdapter extends ArrayAdapter<String> {

    public SListViewAdapter(Context context, List<String> values) {
        super(context, -1, values);
    }
}
