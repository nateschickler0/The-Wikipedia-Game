# The-Wikipedia-Game
A simple, single-class program written in an afternoon that finds a path between two Wikipedia articles using only the links on the page.

The program contains an input parser, link parser, article data structure, and search algorithm. It runs in the console. 
The search is bottlenecked by internet speed, so it may take awhile if many articles need to be accessed.

**Sample Output**
```
Find a path between two wikipedia articles:
Use non-article navigation template links? y/n 
(Off by default, may run faster if y. Input H for details)
Y

Enter the start article URL or title: 
douglas adams

Enter the end article URL or title: 
america

Finding shortest path from 
    https://en.wikipedia.org/wiki/Douglas_Adams 
to 
    https://en.wikipedia.org/wiki/United_States ...

Using cached article Douglas_Adams
747 new links found in article Douglas_Adams
73 new links found in article Author
15 new links found in article Bruce_Daniels


https://en.wikipedia.org/wiki/United_States
FOUND 2 ARTICLE(S) AWAY AFTER PROCESSING 3 ARTICLES(S) AND 835 LINK(S)!
  ---> https://en.wikipedia.org/wiki/United_States
  ^--- https://en.wikipedia.org/wiki/Bruce_Daniels
  ^--- https://en.wikipedia.org/wiki/Douglas_Adams
Connected to a total of 4 Wikipedia article(s) during this search and used 1 cached article(s).
Search ran in 350 miliseconds.

-----------------------------------------------------------------------------------------

```
