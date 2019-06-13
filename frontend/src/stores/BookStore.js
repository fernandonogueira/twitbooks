import { observable, runInAction, computed } from 'mobx'

class BookStore {
    @observable apiData = {};
    @observable currentPage = 0;
    @observable totalPages = 0;

    @computed get count() {
        return this.totalPages * 50;
    }

    @computed get data() {
        return this.apiData[this.currentPage];
    }

    client: any;

    constructor(client) {
        this.client = client;
    }

    setCurrentPage(page) {
        runInAction(() => {
            this.currentPage = page - 1;
            this.getBooks();
        })
    }

    ignoreBook(book) {
    }

    getBooks() {
        if (this.apiData[this.currentPage]) {
            return;
        }
        this.client.get(`/books?page=${this.currentPage}`)
            .then(response => {
                runInAction(() => {
                    const data = response.data;
                    const content = response.data.content;
                    this.totalPages = data.totalPages;
                    this.apiData[this.currentPage] = content;
                });
            })
            .catch(e => {
                // TODO proper error handling
            })
    }

}

export default BookStore;