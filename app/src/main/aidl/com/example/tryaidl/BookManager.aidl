// BookManager.aidl


// Declare any non-default types here with import statements

// BookManager.aidl
//第二类AIDL文件
//作用是定义方法接口
//导入所需要使用的非默认支持数据类型的包
package com.example.tryaidl;
import com.example.tryaidl.Book;
import com.example.tryaidl.IOnNewBookArrivedListener;

interface BookManager {

    //所有的返回值前都不需要加任何东西，不管是什么数据类型
    List<Book> getBooks();

    //传参时除了Java基本类型以及String，CharSequence之外的类型
    //都需要在前面加上定向tag，具体加什么量需而定
    void addBook(in Book book);

    void registerListener(IOnNewBookArrivedListener listener);

    void unregisterListener(IOnNewBookArrivedListener listener);
}
