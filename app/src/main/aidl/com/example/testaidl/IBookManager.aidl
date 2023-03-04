// IBookManager.aidl
package com.example.testaidl;

// Declare any non-default types here with import statements
import com.example.testaidl.IOnNewBookArrivedListener;
import com.example.testaidl.Book;

interface IBookManager {
List<Book> getBookList();
void addBook(in Book book);
void registerListener(IOnNewBookArrivedListener l);
void unRegisterListener(IOnNewBookArrivedListener l);
}