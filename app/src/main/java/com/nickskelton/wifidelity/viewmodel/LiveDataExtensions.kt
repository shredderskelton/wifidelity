package com.nickskelton.wifidelity.viewmodel

import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

fun <T> Flowable<T>.toLiveData(): LiveData<T> = LiveDataReactiveStreams.fromPublisher(this)

fun <T> Flowable<T>.toLiveEvent(): LiveEvent<T> = PublisherLiveEvent(this)

fun <T> Observable<T>.toLiveData(
    backpressureStrategy: BackpressureStrategy = BackpressureStrategy.LATEST
): LiveData<T> = LiveDataReactiveStreams.fromPublisher(this.toFlowable(backpressureStrategy))

fun <T> Observable<T>.toLiveEvent(
    backpressureStrategy: BackpressureStrategy = BackpressureStrategy.LATEST
): LiveEvent<T> = PublisherLiveEvent(this.toFlowable(backpressureStrategy))

fun <T> Single<T>.toLiveData(): LiveData<T> =
    LiveDataReactiveStreams.fromPublisher(this.toFlowable())

fun <T> Single<T>.toLiveEvent(): LiveEvent<T> =
    PublisherLiveEvent(this.toFlowable())

fun <T> Maybe<T>.toLiveData(): LiveData<T> =
    LiveDataReactiveStreams.fromPublisher(this.toFlowable())

fun <T> Maybe<T>.toLiveEvent(): LiveEvent<T> = PublisherLiveEvent(this.toFlowable())

fun Completable.toLiveData(): LiveData<Unit> =
    LiveDataReactiveStreams.fromPublisher(this.toFlowable())

fun Completable.toLiveEvent(): LiveEvent<Unit> = PublisherLiveEvent(this.toFlowable())

fun <T> LiveData<T>.observe(owner: LifecycleOwner, observer: (T?) -> Unit): Observer<T> {
    val archObserver = Observer<T> { observer(it) }
    this.observe(owner, archObserver)
    return archObserver
}

fun <T> LiveData<T>.observeNonNull(owner: LifecycleOwner, observer: (T) -> Unit): Observer<T> {
    val archObserver = Observer<T> { it?.let(observer) }
    this.observe(owner, archObserver)
    return archObserver
}

fun <T : Any, L : LiveData<T>> LifecycleOwner.observe(liveData: L, body: (T?) -> Unit) {
    liveData.observe(this, Observer(body))
}

fun <T : Any, L : LiveData<T>> LifecycleOwner.observeNonNull(liveData: L, body: (T) -> Unit) {
    liveData.observe(this) { it?.let(body) }
}

fun <T : Any, L : LiveData<T>> LifecycleOwner.reObserveNonNull(liveData: L, body: (T) -> Unit) {
    liveData.removeObservers(this)
    observeNonNull(liveData, body)
}

fun <T : Any?> MutableLiveData<T>.default(initialValue: T) = apply { setValue(initialValue) }

/**
 * Copied from [androidx.lifecycle.LiveDataReactiveStreams.PublisherLiveData]
 * but extending LiveEvent
 */
private class PublisherLiveEvent<T>(private val publisher: Publisher<T>) : LiveEvent<T>() {
    val subscriber: AtomicReference<LiveDataSubscriber> = AtomicReference()

    override fun onActive() {
        super.onActive()
        val newSubscriber = LiveDataSubscriber()
        subscriber.set(newSubscriber)
        publisher.subscribe(newSubscriber)
    }

    override fun onInactive() {
        super.onInactive()
        subscriber.getAndSet(null)?.cancelSubscription()
    }

    inner class LiveDataSubscriber : AtomicReference<Subscription>(), Subscriber<T> {

        override fun onSubscribe(subscription: Subscription) {
            if (compareAndSet(null, subscription)) {
                subscription.request(Long.MAX_VALUE)
            } else {
                subscription.cancel()
            }
        }

        override fun onNext(item: T) = postValue(item)

        override fun onError(exception: Throwable) {
            subscriber.compareAndSet(this, null)

            val task = {
                throw RuntimeException("LiveEvent does not handle errors. Errors from " +
                    "publishers should be handled upstream and propagated as " +
                    "state", exception)
            }
            if (Looper.myLooper() == Looper.getMainLooper()) {
                task()
            } else {
                Handler(Looper.getMainLooper()).post(task)
            }
        }

        override fun onComplete() {
            subscriber.compareAndSet(this, null)
        }

        fun cancelSubscription() = get()?.cancel()
    }
}
/*
 *  Copyright 2017 Google Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * A lifecycle-aware observable that sends only new updates after subscription, used for events like
 * navigation and Snackbar messages.
 *
 *
 * This avoids a common problem with events: on configuration change (like rotation) an update
 * can be emitted if the observer is active. This LiveData only calls the observable if there's an
 * explicit call to setValue() or call().
 *
 *
 * Note that only one observer can be registered at any given time
 */
abstract class LiveEvent<T> : LiveData<T>() {

    private val pending = AtomicBoolean(false)

    private var currentWrappedObserver: Observer<T>? = null

    @MainThread
    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        super.observe(owner, wrap(observer))
    }

    @MainThread
    override fun observeForever(observer: Observer<in T>) {
        super.observeForever(wrap(observer))
    }

    @Synchronized
    private fun wrap(observer: Observer<in T>): Observer<in T> {
        if (hasObservers()) {
            throw IllegalStateException("LiveEvent may not have multiple observers")
        }
        val wrappedObserver = Observer<T> {
            if (pending.compareAndSet(true, false)) {
                observer.onChanged(it)
            }
        }
        currentWrappedObserver = wrappedObserver
        return wrappedObserver
    }

    @Synchronized
    @MainThread
    override fun removeObserver(observer: Observer<in T>) {
        currentWrappedObserver?.let { super.removeObserver(it) }
        currentWrappedObserver = null
    }

    @MainThread
    override fun setValue(value: T?) {
        pending.set(true)
        super.setValue(value)
    }
}
