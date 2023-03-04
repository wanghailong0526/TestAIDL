// IOnNewBookReceivedListener.aidl
package com.example.testaidl;

// Declare any non-default types here with import statements
import com.example.testaidl.Book;

interface IOnNewBookArrivedListener {
void onNewBookArrived(in Book book);
}