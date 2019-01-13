package com.example.aidlserver;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;
import com.example.tryaidl.Book;
import com.example.tryaidl.BookManager;
import com.example.tryaidl.IOnNewBookArrivedListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class AIDLService extends Service {
    public final String TAG = this.getClass().getSimpleName();
    //包含Book对象的list,支持多线程访问下的自动并发同步处理
    private CopyOnWriteArrayList<Book> mBooks = new CopyOnWriteArrayList<Book>();
    private AtomicBoolean mIsServiceDestroyed = new AtomicBoolean(false);

    //private CopyOnWriteArrayList<IOnNewBookArrivedListener> mListenerlist = new CopyOnWriteArrayList<IOnNewBookArrivedListener>(); //改种写法会导致客户端传递同一个aidl接口对象，到服务端是重新生成的两个不同的对象，所以考虑如下方法，使用remotecallbackList
    private RemoteCallbackList<IOnNewBookArrivedListener> mListenerlist = new RemoteCallbackList<IOnNewBookArrivedListener>();
    public AIDLService() {}

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.e(getClass().getSimpleName(), String.format("on bind,intent = %s", intent.toString()));
        return mbookManager;
    }

    //由AIDL文件生成的BookManager
    private final BookManager.Stub mbookManager = new BookManager.Stub() {

        @Override
        public List<Book> getBooks() throws RemoteException {
            synchronized (this) {
                //SystemClock.sleep(5000);
                Log.e(TAG, "invoking getBooks() method , now the list is : " + mBooks.toString());
                if (mBooks != null) {
                    return mBooks;
                }

                return new CopyOnWriteArrayList<Book>();
            }
        }

        @Override
        public void addBook(Book book) throws RemoteException {
            synchronized (this) {
                if (mBooks == null) {
                    mBooks = new CopyOnWriteArrayList<Book>();
                }
                if (book == null) {
                    Log.e(TAG, "Book is null in In");
                    book = new Book();
                }
                Log.e(TAG, "invoking addBooks() method , now the list is : " + book.toString());
                //尝试修改book的参数，主要是为了观察其到客户端的反应
                //book.setPrice(2333);
                if (!mBooks.contains(book)) {
                    mBooks.add(book);
                }
                //打印mBooks列表，观察客户端传过来的mBooks
                Log.e(TAG, "invoking addBooks() method , now the list is : " + mBooks.toString());
            }
        }

    /*
    @Override
    public void registerListener(IOnNewBookArrivedListener listener) throws RemoteException {
        Log.d(TAG, "registerListener listener: " + listener);
        if(!mListenerlist.contains(listener)){
            mListenerlist.add(listener);
        }else{
            Log.d(TAG, "registerListener: the listener already exits");
        }
        Log.d(TAG, "registerListener size: " + mListenerlist.size());
    }*/

        @Override
        public void registerListener(IOnNewBookArrivedListener listener) throws RemoteException {
            mListenerlist.register(listener);
        }

    /*
    @Override
    public void unregisterListener(IOnNewBookArrivedListener listener) throws RemoteException {
        if(mListenerlist.contains(listener)){
            mListenerlist.remove(listener);
        }else{
            Log.d(TAG, "unregisterListener: the listener can not found");
        }
            Log.d(TAG, "registerListener size: " + mListenerlist.size());
        }
    };*/

        @Override
        public void unregisterListener(IOnNewBookArrivedListener listener) throws RemoteException {
            mListenerlist.unregister(listener);
        }
    };

    private void onNewBookArrived(Book book) throws RemoteException {
        Log.d(TAG, "onNewBookArrived: " + book.toString());
        if (!mBooks.contains(book))
            mBooks.add(book);
        final int N = mListenerlist.beginBroadcast();//size
        //Log.d(TAG, "onNewBookArrived: notify Listeners " + mListenerlist.beginBroadcast());
        for(int i = 0 ; i < N; i++ ){
            IOnNewBookArrivedListener listener = mListenerlist.getBroadcastItem(i);//get(i)
                if(listener != null ) {
                    listener.onNewBookArrived(book);
            }
        }
        mListenerlist.finishBroadcast();
    }

    @Override
    public void onCreate() {
        Book book = new Book();
        book.setName("Android开发艺术探索");
        book.setPrice(28);
        mBooks.add(book);
        Log.d("AIDLServer", "add a book");
        new Thread(new ServiceWorker()).start();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "AIDLService onDestroy: ");
        //super.onDestroy();
        mIsServiceDestroyed.set(true);
    }

    private class ServiceWorker implements Runnable{
        @Override
        public void run() {
            // do background processing here .....
            while(!mIsServiceDestroyed.get()){
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                int bookid = mBooks.size() - 1 ;
                Book book = new Book();
                book.setName("Book" + bookid);
                book.setPrice(bookid);
                try {
                    onNewBookArrived(book);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
