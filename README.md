# TwitBooks

## About

Twitbooks is an app that extracts book suggestions from your Twitter feed.
It uses [Named Entity Recognition](https://spacy.io/usage/linguistic-features#named-entities)
to extract book names from your feed and uses either [Goodreads](`https://www.goodreads.com/)
or [Google Books](https://books.google.com/) to fetch book data.

## Architecture

The structure is simple. The `backend` is a Spring Boot project that interfaces with the Twitter API
and saves Twitter data in a Postgres database. A job periodically hits the `ner` service which is a
Flask application that uses `spaCy` which contains a pre trained model for identifying works of art
through named entity recognition. The `ner` service exposes a single API which receives the message
and returns any book names that could be extracted from it.

The `frontend` directory contains a React client app for visualizing the books that were found.

## Running The Application

### Configure the Profile

You have a few options for both NER and for the books API. You specify which version of each
through Spring profiles. Here's an example using `spaCy` for NER and Google Books for book data:

edit `application.properties` on the `backend` project and edit `active.profiles` to
```
dev,spacy,google
```

For NER you have the option to use `spaCy` or [AWS Comprehend](https://aws.amazon.com/comprehend/) by using either the `spacy` or the `aws` profiles

For the books API you can use either `Goodreads` or `Google Books` by using either the `google` or the `goodreads` profiles.
 
The specific configuration for each profile (spacy, aws, google, goodreads) is described in their specific section.

### Postgres Setup

A Postgres is used to store messages, user and book information. Spin up an instance by running `docker-compose up -d` on the
project root.

### Add Twitter API Keys

To read information from your Twitter account you need to create API keys.

### Goodreads Configuration

To use Goodreads you need to request a developer API [Here](https://www.goodreads.com/api/keys). Then put your
key on the file `application-goodreads.properties` (or use the `GOODREADS_KEY` env variable).

NOTE: Goodreads imposes a limitation of 1 request per second which makes processing books quite slow, so keep
that in mind if you're going to use it.

### Google Books Setup

The API used by Google Books doesn't need authentication. So just setting `google` on the active profiles and
it will work.

NOTE: I haven't found anywhere saying about rate limits for this API. During my tests it does have some
throttling. The application itself doesn't handle rate limiting for this API, like it does for Goodreads,
so be careful when using it!

### SpaCy Setup

Spin up the NER container using `docker-compose up -d` and configure the services address on
`application-spacy.properties` or use the `NER_CLIENT_URL` env variable. If you're on linux the
default value: `http://localhost:5000/` should work out of the box.

### AWS Comprehend Setup

From their website: `Amazon Comprehend is a machine learning powered service that makes it easy to find insights and relationships in text.`
As per usual, you need to provide access keys for consuming AWS services. Once you have your keys either edit the file `application-comprehend.properties`
or have the `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` and `AWS_REGION` environment variables.
