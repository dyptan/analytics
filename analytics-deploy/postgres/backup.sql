--
-- PostgreSQL database dump
--

-- Dumped from database version 17.4 (Debian 17.4-1.pgdg120+2)
-- Dumped by pg_dump version 17.4 (Debian 17.4-1.pgdg120+2)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: user_roles; Type: TABLE; Schema: public; Owner: ivan
--

CREATE TABLE public.user_roles (
    user_username character varying(255) NOT NULL,
    roles smallint,
    CONSTRAINT user_roles_roles_check CHECK (((roles >= 0) AND (roles <= 1)))
);


ALTER TABLE public.user_roles OWNER TO ivan;

--
-- Name: users; Type: TABLE; Schema: public; Owner: ivan
--

CREATE TABLE public.users (
    username character varying(255) NOT NULL,
    password character varying(255)
);


ALTER TABLE public.users OWNER TO ivan;

--
-- Data for Name: user_roles; Type: TABLE DATA; Schema: public; Owner: ivan
--

COPY public.user_roles (user_username, roles) FROM stdin;
admin	0
test	1
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: ivan
--

COPY public.users (username, password) FROM stdin;
admin	$2a$10$Mo/M3puihbCnLjUhj/1D/ei7oxUa7cR/t10auYCos7rfhh5A/PvTi
test	$2a$10$weCDsdz8C4l5.n2NYpl2QuV20MLnX1KDLdUusqltFBlT/Pa5PM68S
\.


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: ivan
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (username);


--
-- Name: user_roles fks9rxtuttxq2ln7mtp37s4clce; Type: FK CONSTRAINT; Schema: public; Owner: ivan
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT fks9rxtuttxq2ln7mtp37s4clce FOREIGN KEY (user_username) REFERENCES public.users(username);


--
-- PostgreSQL database dump complete
--

