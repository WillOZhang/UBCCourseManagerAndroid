package will.ubccoursemanager.CourseSchedule.CourseScheduleManager;

import android.support.annotation.NonNull;

import java.io.Serializable;
import java.sql.Time;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import will.ubccoursemanager.CourseSchedule.CourseScheduleManager.Exceptions.InstructorTBAException;
import will.ubccoursemanager.CourseSchedule.CourseScheduleManager.Exceptions.NoScheduledMeetingException;

/**
 * Created by Will on 2017/5/22.
 */
public class Section implements Serializable, Comparable<Section> {
    private Course course;
    private String section, status, activity;
    private Set<String> days;
    private Instructor instructor;
    private Classroom classroom;
    private Time start, end;
    private String term;
    private Map<String, List<Time>> timeMap;
    private int totalSeats, currentRegistered, restrictSeats, generalSeats;
    private String restrictTo;
    private String lastWithdraw;

    public Section(Course course, String section, String status, String activity,
                   Instructor instructorName, Classroom classroom,
                   String term) {
        this.activity = activity;
        this.section = section;
        this.course = course;
        this.status = status;
        this.instructor = instructorName;
        this.classroom = classroom;
        this.term = term;
        this.restrictTo = "";
        timeMap = new HashMap<String, List<Time>>();
        days = new HashSet<>();

        course.addSection(this);
    }

    public void setInstructor(Instructor instructor) {
        instructor.addSection(this);
        Instructor instructor1 = InstructorManager.getInstance().getInstructor(instructor);
        this.instructor = instructor1;
        course.addSection(this);
    }

    public void setClassroom(Classroom classroom) {
        BuildingManager.getInstance().getBuilding(classroom.getBuildingThatThisClassroomAt());
        this.classroom = classroom;
        course.addSection(this);
    }

    public void setSeatsInfo(int totalSeats, int currentRegistered, int restrictSeats, int generalSeats) {
        this.totalSeats = totalSeats;
        this.currentRegistered = currentRegistered;
        this.restrictSeats = restrictSeats;
        this.generalSeats = generalSeats;
    }

    public void setRestrictTo(String restrictTo) {
        this.restrictTo = restrictTo;
    }

    public void setLastWithdraw(String lastWithdraw) {
        this.lastWithdraw = lastWithdraw;
    }

    public void addTime(String days, Time start, Time end) {
        this.days.add(days);

        List<Time> times = timeMap.get(days);
        if (times != null) {
            times.add(start);
            times.add(end);
        } else {
            List<Time> temp = new ArrayList<>();
            temp.add(start);
            temp.add(end);
            timeMap.put(days,temp);
        }
    }

    public void changeToSameSection(Section section) {
        this.status = section.getStatus();
        this.activity = section.getActivity();
        this.term = section.getTerm();
        this.totalSeats = section.totalSeats;
        this.currentRegistered = section.currentRegistered;
        this.restrictSeats = section.restrictSeats;
        this.generalSeats = section.generalSeats;
        this.restrictTo = section.restrictTo;
        this.lastWithdraw = section.lastWithdraw;
        try {
            this.days = section.getDays();
            this.classroom = section.getClassroom();
            this.start = section.getStart();
            this.end = section.getEnd();
            this.timeMap = section.getTimeMap();
        } catch (NoScheduledMeetingException ignored) {

        }

        try {
            this.instructor = section.getInstructor();
        } catch (InstructorTBAException ignored) {

        }
    }

    public String getSection() {
        return section;
    }

    public Course getCourse() {
        return course;
    }

    public String getStatus() {
        return status;
    }

    public String getActivity() {
        return activity;
    }

    public Set<String> getDays() throws NoScheduledMeetingException {
        if (days.size() == 0 || start == null || end == null)
            throw new NoScheduledMeetingException();
        return days;
    }

    public Time getStart() {
        return start;
    }

    public Time getEnd() {
        return end;
    }

    public String getTerm() {
        return term;
    }

    public Map<String, List<Time>> getTimeMap() {
        return timeMap;
    }

    public Classroom getClassroom() throws NoScheduledMeetingException {
        if (classroom == null)
            throw new NoScheduledMeetingException();
        return classroom;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Section)) return false;

        Section section1 = (Section) o;

        if (course != null ? !course.equals(section1.course) : section1.course != null) return false;
        return section != null ? section.equals(section1.section) : section1.section == null;
    }

    @Override
    public int hashCode() {
        int result = course != null ? course.hashCode() : 0;
        result = 31 * result + (section != null ? section.hashCode() : 0);
        return result;
    }

    public Instructor getInstructor() throws InstructorTBAException {
        if (instructor == null)
            throw new InstructorTBAException();
        return instructor;
    }

    public void print() {
        System.out.println("\t ^Section: " + section + " " + status + " " + activity + " " + term + "^");
        if (instructor != null)
            System.out.println("\t %Instructor: " + instructor.getName() + "%");
        if (days != null) {
            for (Map.Entry<String, List<Time>> entry : timeMap.entrySet()) {
                System.out.println("\t *Days: " + entry.getKey() + "*");
                for (Time time : entry.getValue())
                    System.out.println("\t &Time: " + time.toString() + "&");
            }
        }
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public int getCurrentRegistered() {
        return currentRegistered;
    }

    public int getRestrictSeats() {
        return restrictSeats;
    }

    public int getGeneralSeats() {
        return generalSeats;
    }

    public String getRestrictTo() {
        return restrictTo;
    }

    public String getLastWithdraw() {
        return lastWithdraw;
    }

    public void setDays(Set<String> days) {
        this.days = days;
    }

    public void setTimeMap(Map<String, List<Time>> timeMap) {
        this.timeMap = timeMap;
    }

    @Override
    public int compareTo(@NonNull Section o) {
        // TODO: find a better way to compare sections
        // worth to try

//        Pattern p1 = Pattern.compile("\\d+");
//        Pattern p2 = Pattern.compile("(\\d+)([a-zA-Z])");
//        Pattern p3 = Pattern.compile("([a-zA-Z])(\\d+)");
//        Pattern p4 = Pattern.compile("([a-zA-Z])(\\d+)([a-zA-Z])");
//
//        Matcher tm1 = p1.matcher(this.section);
//        if (tm1.find()) { // TODO: L1E can also be true under this condistion
//            Matcher om1 = p1.matcher(o.section);
//            if (om1.find()) {
//                return Integer.parseInt(tm1.group()) - Integer.parseInt(om1.group());
//            }
//        }



        Pattern pattern = Pattern.compile("[a-zA-Z]*");
        Pattern number = Pattern.compile("\\d*");

        Matcher thisCourseNumLetter = pattern.matcher(this.section);
        Matcher thisCourseNumber = number.matcher(this.section);

        Matcher oCourseNumLetter = pattern.matcher(o.section);
        Matcher oCourseNumber = number.matcher(o.section);

        String s1 = null;
        String s2 = null;

        while (thisCourseNumLetter.find())
            s1 = thisCourseNumLetter.group();
        while (oCourseNumLetter.find())
            s2 = oCourseNumLetter.group();

        int i1 = 0;
        int i2 = 0;

        while (thisCourseNumber.find()) {
            String temp = thisCourseNumber.group();
            if (!temp.isEmpty())
                i1 = Integer.parseInt(temp);
        }
        while (oCourseNumber.find()) {
            String temp = oCourseNumber.group();
            if (!temp.isEmpty())
                i2 = Integer.parseInt(temp);
        }

        if (s1 == null && s2 == null)
            return i1 - i2;
        else if (s1 != null && s2 != null) {
            if (s1.equals(s2))
                return i1 - i2;
            else
                return s1.compareTo(s2);
        } else {
            if (s1 == null)
                return 0;
            else
                return i1;
        }
    }
}
