package will.ubccoursemanager.SupportUI;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

import will.ubccoursemanager.CourseSchedule.CourseScheduleManager.Course;
import will.ubccoursemanager.R;

public class CourseView extends AppCompatActivity {
    private List<String> dataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_courses_view);

        Intent intent = this.getIntent();
        dataList = intent.getStringArrayListExtra("list");
        Course course = (Course) intent.getSerializableExtra("course");

        TextView courseName = (TextView) findViewById(R.id.courseName);
        TextView description = (TextView) findViewById(R.id.courseDescription);

        courseName.setText(course.getDepartment().getShortName() + " " + course.getCourseNumber() + " " + course.getCourseName());
        description.setText(course.getReqs() + "\n" + course.getDescription());
        description.setMovementMethod(new ScrollingMovementMethod());

        ListView listView = (ListView) findViewById(R.id.sections);
        ListViewAdapter listViewAdapter = new ListViewAdapter(this, dataList);
        listView.setAdapter(listViewAdapter);
    }

    public void cancel(View view) {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
    }
}
