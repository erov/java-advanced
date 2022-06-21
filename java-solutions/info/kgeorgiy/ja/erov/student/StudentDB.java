package info.kgeorgiy.ja.erov.student;

import info.kgeorgiy.java.advanced.student.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements AdvancedQuery {
    private static final Comparator<Student> STUDENT_COMPARATOR =
            Comparator.comparing(Student::getLastName).reversed()
            .thenComparing(Comparator.comparing(Student::getFirstName).reversed())
            .thenComparing(Student::getId);

    // StudentQuery:
    @Override
    public List<String> getFirstNames(List<Student> students) {
        return getFieldList(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return getFieldList(students, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(List<Student> students) {
        return getFieldList(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return getFieldList(students, StudentDB::getFullName);
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return getFieldSet(students, Student::getFirstName);
    }

    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        return students.stream()
                .max(Comparator.naturalOrder())
                .map(Student::getFirstName)
                .orElse("");
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortedStudentList(students, Comparator.naturalOrder());
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortedStudentList(students, STUDENT_COMPARATOR);
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return getStudentsByPredicateList(students, getEqualityPredicate(Student::getFirstName, name));
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return getStudentsByPredicateList(students, getEqualityPredicate(Student::getLastName, name));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return getStudentsByPredicateList(students, getEqualityPredicate(Student::getGroup, group));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return getStudentsByPredicate(
                students,
                getEqualityPredicate(Student::getGroup, group),
                Collectors.toMap(
                        Student::getLastName,
                        Student::getFirstName,
                        BinaryOperator.minBy(Comparator.naturalOrder())
                )
        );
    }


    // GroupQuery:
    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getGroupList(students, STUDENT_COMPARATOR);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getGroupList(students, Comparator.naturalOrder());
    }

    @Override
    public GroupName getLargestGroup(Collection<Student> students) {
        return getMaxByStatistics(
                students,
                Student::getGroup,
                Collectors.toList(),
                Comparator.naturalOrder()
        ).orElse(null);
    }

    @Override
    public GroupName getLargestGroupFirstName(Collection<Student> students) {
        return getMaxByStatistics(
                students,
                Student::getGroup,
                distinctFieldCollector(Student::getFirstName),
                Comparator.reverseOrder()
        ).orElse(null);
    }


    // AdvancedQuery:
    @Override
    public String getMostPopularName(Collection<Student> students) {
        return getMaxByStatistics(
                students,
                Student::getFirstName,
                distinctFieldCollector(Student::getGroup),
                Comparator.naturalOrder()
        ).orElse("");
    }

    @Override
    public List<String> getFirstNames(Collection<Student> students, int[] indices) {
        return getIndexedFieldList(students, indices, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(Collection<Student> students, int[] indices) {
        return getIndexedFieldList(students, indices, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(Collection<Student> students, int[] indices) {
        return getIndexedFieldList(students, indices, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(Collection<Student> students, int[] indices) {
        return getIndexedFieldList(students, indices, StudentDB::getFullName);
    }


    private static String getFullName(Student student) {
        return String.format("%s %s", student.getFirstName(), student.getLastName());
    }

    private <T, R> R getField(Stream<Student> studentStream,
                              Function<Student, T> fieldGetter,
                              Collector<T, ?, R> collector) {
        return studentStream.map(fieldGetter)
                .collect(collector);
    }

    private <T> List<T> getFieldList(Collection<Student> students,
                                     Function<Student, T> fieldGetter) {
        return getField(students.stream(), fieldGetter, Collectors.toList());
    }

    private <T> Set<T> getFieldSet(Collection<Student> students,
                                    Function<Student, T> fieldGetter) {
        return getField(students.stream(), fieldGetter, Collectors.toCollection(TreeSet::new));
    }

    private <T> List<T> getIndexedFieldList(Collection<Student> students,
                                            int[] indices,
                                            Function<Student, T> fieldGetter) {
        return getIndexedFieldList(students.stream().toList(), indices, fieldGetter);
    }

    private <T> List<T> getIndexedFieldList(List<Student> students,
                                            int[] indices,
                                            Function<Student, T> fieldGetter) {
        return getField(
                Arrays.stream(indices).mapToObj(students::get),
                fieldGetter,
                Collectors.toList()
        );
    }


    private Stream<Student> sortedStudentStream(Collection<Student> students,
                                                Comparator<Student> comparator) {
        return students.stream()
                .sorted(comparator);
    }

    private List<Student> sortedStudentList(Collection<Student> students,
                                            Comparator<Student> comparator) {
        return sortedStudentStream(students, comparator).toList();
    }


    private <T> Predicate<Student> getEqualityPredicate(Function<Student, T> fieldGetter, T value) {
        return student -> Objects.equals(value, fieldGetter.apply(student));
    }

    private <C> C getStudentsByPredicate(Collection<Student> students,
                                         Predicate<Student> predicate,
                                         Collector<Student, ?, C> collector) {
        return sortedStudentStream(students, STUDENT_COMPARATOR).collect(Collectors.filtering(predicate, collector));
    }

    private List<Student> getStudentsByPredicateList(Collection<Student> students,
                                                     Predicate<Student> predicate) {
        return getStudentsByPredicate(students, predicate, Collectors.toList());
    }


    private <K, V> Stream<Map.Entry<K, List<V>>> statisticsStream(Stream<Student> studentStream,
                                                                  Function<Student, K> fieldGetter,
                                                                  Collector<Student, ?, List<V>> studentCollector) {
        return studentStream.collect(Collectors.groupingBy(fieldGetter, TreeMap::new, studentCollector))
                .entrySet()
                .stream();
    }

    private <K, V> Optional<K> getMaxByStatistics(Collection<Student> students,
                                                  Function<Student, K> fieldGetter,
                                                  Collector<Student, ?, List<V>> studentCollector,
                                                  Comparator<K> fieldComparator) {
        return statisticsStream(students.stream(), fieldGetter, studentCollector)
                .max(Comparator.comparing(
                        Map.Entry<K, List<V>>::getValue,
                        Comparator.comparing(List::size)
                ).thenComparing(Map.Entry.comparingByKey(fieldComparator)))
                .map(Map.Entry::getKey);
    }

    private List<Group> getGroupList(Collection<Student> students,
                                     Comparator<Student> studentComparator) {
        return statisticsStream(sortedStudentStream(students, studentComparator), Student::getGroup, Collectors.toList())
                .map(entry -> new Group(entry.getKey(), entry.getValue()))
                .toList();
    }

    private <T> Collector<Student, ?, List<T>> distinctFieldCollector(Function<Student, T> fieldGetter) {
        return Collectors.collectingAndThen(
                Collectors.mapping(fieldGetter, Collectors.toSet()),
                ArrayList::new
        );
    }
}
