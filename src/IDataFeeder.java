public interface IDataFeeder<T> {

    // return null if there's nothing more to feed
    T getData();
}
