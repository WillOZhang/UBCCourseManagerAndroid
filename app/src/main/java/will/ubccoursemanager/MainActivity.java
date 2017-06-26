package will.ubccoursemanager;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import will.ubccoursemanager.CourseSchedule.CourseScheduleManager.BuildingManager;
import will.ubccoursemanager.CourseSchedule.CourseScheduleManager.Course;
import will.ubccoursemanager.CourseSchedule.CourseScheduleManager.CourseManager;
import will.ubccoursemanager.CourseSchedule.CourseScheduleManager.Department;
import will.ubccoursemanager.CourseSchedule.CourseScheduleManager.InstructorManager;
import will.ubccoursemanager.CourseSchedule.CourseScheduleManager.Section;
import will.ubccoursemanager.CourseSchedule.ReadData;
import will.ubccoursemanager.CourseSchedule.ReadJson;
import will.ubccoursemanager.SupportUI.FDCListViewAdapter;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "Tag";
    public static final String ERROR = "Error";

    private ReadData readData;
    private CourseManager courseManager;
    private BuildingManager buildingManager;
    private InstructorManager instructorManager;

    private FDCListViewAdapter adapter;
    private ListView listView;
    private List<String> dataList;

    private List<String> facultyList;

    private String faculty;
    private Department departmentChosen;
    private Course courseChosen;
    private Section sectionChosen;
    private Context context = this;
    private int COURSE_ID = 1000;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    listView.setVisibility(View.VISIBLE);
                    assignOnItemClickListener();
                    return true;

                case R.id.navigation_dashboard:
                    listView.setVisibility(View.INVISIBLE);
                    return true;

                case R.id.navigation_notifications:
                    listView.setVisibility(View.INVISIBLE);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dataList = new ArrayList<>();
        handleFaculties();
        Log.i(TAG, "Finished handle faculty data");

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        listView = (ListView) findViewById(R.id.faculty_list);
        adapter = new FDCListViewAdapter(this, dataList);
        listView.setAdapter(adapter);
        assignOnItemClickListener();
        Log.i(TAG, "Created a screen");
    }

    private void assignOnItemClickListener() {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (faculty == null) {
                    faculty = (String) parent.getItemAtPosition(position);

                    if (!courseManager.getFinishedParseFaculties().contains(faculty)) {

                        List<String> temp = courseManager.getDepartmentsByFaculty(faculty);
                        for (String name : temp) {
                            ReadJson.departmentReader(convertToJsonString(name));
                        }
                        changeData(courseManager.getDepartments(faculty));

                    } else
                        changeData(courseManager.getDepartments(faculty));

                    Log.i(TAG, "Faculty: " + faculty + " has been chosen");

                    adapter.notifyDataSetChanged();

                } else if (departmentChosen == null){
                    String temp = (String) parent.getItemAtPosition(position);
                    String departmentShortName = temp.split("@")[2];
                    departmentChosen = courseManager.getDepartment(departmentShortName);
                    List<String> coursesName = courseManager.getCoursesByDepartment(departmentChosen);
                    for (String courseName : coursesName) {
                        ReadJson.courseReader(departmentChosen, convertToJsonString(courseName));
                    }
                    coursesName = departmentChosen.getCoursesForDisplay();
                    if (coursesName.size() > 0)
                        changeData(coursesName);
                    else {
                        coursesName.add("No course is offered in this department");
                        changeData(coursesName);
                    }

                    Log.i(TAG, "Department: " + departmentShortName + " has been chosen");

                    adapter.notifyDataSetChanged();

                } else if (courseChosen == null) {
                    String temp = (String) parent.getItemAtPosition(position);
                    try {
                        courseChosen = departmentChosen.getCourse(temp.split("@")[2]);
                        List<String> displaySections = new ArrayList<String>();
                        List<String> tempList = courseChosen.getSectionsList();
                        if (tempList.size() > 0) {
                            tempList = courseManager.getSectionsByCourse(courseChosen);
                            for (String section : tempList)
                                ReadJson.sectionReader(courseChosen, convertToJsonString(section));
                            for (String s : courseChosen.getSectionsForDisplay())
                                displaySections.add(s);
                        } else {
                            displaySections.add("No section for the selected course");
                        }

                        // TODO: add new intent
                        Intent displayCourse = new Intent(context, CourseView.class);
                        ArrayList listForIntent = new ArrayList(displaySections);
                        displayCourse.putExtra("course", courseChosen);
                        displayCourse.putStringArrayListExtra("list", listForIntent);
                        startActivityForResult(displayCourse, COURSE_ID);

                        Log.i(TAG, "Course: " + departmentChosen.getShortName() +  courseChosen.getCourseNumber() + " has een chosen");

                        //adapter.notifyDataSetChanged();
                    } catch (IndexOutOfBoundsException e) {
                        Log.i(ERROR, e.getMessage() + temp);
                    }
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (courseChosen != null) {
            changeData(departmentChosen.getCoursesForDisplay());
            adapter.notifyDataSetChanged();
            courseChosen = null;
        } else if (departmentChosen != null) {
            changeData(courseManager.getDepartments(faculty));
            adapter.notifyDataSetChanged();
            departmentChosen = null;
        } else if (faculty != null) {
            changeData(facultyList);
            adapter.notifyDataSetChanged();
            faculty = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_CANCELED) {
            courseChosen = null;
        }
    }

    private void handleFaculties() {
        facultyList = new ArrayList<>();
        try {
            for (String faculty : getAssets().list("faculties")) {
                StringBuilder stringBuilder = new StringBuilder();
                InputStreamReader inputStreamReader = new InputStreamReader(getAssets().open(faculty), "UTF-8");
                BufferedReader br = new BufferedReader(inputStreamReader);
                String line = br.readLine();
                while (line != null) {
                    stringBuilder.append(line);
                    line = br.readLine();
                }
                br.close();
                inputStreamReader.close();

                String temp = faculty.split(".json")[0];
                facultyList.add(temp);
                List<String> departments = ReadJson.facultyReader(stringBuilder.toString());
                CourseManager.getInstance().addFDPair(temp, departments);
            }
            changeData(facultyList);
        } catch (IOException ignored) {}

        courseManager = CourseManager.getInstance();
        buildingManager = BuildingManager.getInstance();
        instructorManager = InstructorManager.getInstance();
    }

    private String convertToJsonString(String jsonFilePath) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(getAssets().open(jsonFilePath + ".json"), "UTF-8");
            BufferedReader br = new BufferedReader(inputStreamReader);
            String line = br.readLine();
            while (line != null) {
                stringBuilder.append(line);
                line = br.readLine();
            }
            br.close();
            inputStreamReader.close();
        } catch (IOException ignored) {}
        Log.i(TAG, "Convert " + jsonFilePath + "to json string");
        return stringBuilder.toString();
    }

    private void changeData(List<String> data) {
        dataList.clear();
        for (String string : data)
            dataList.add(string);
    }
}
