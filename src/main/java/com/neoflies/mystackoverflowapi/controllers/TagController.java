package com.neoflies.mystackoverflowapi.controllers;

import com.neoflies.mystackoverflowapi.domains.Tag;
import com.neoflies.mystackoverflowapi.domains.User;
import com.neoflies.mystackoverflowapi.dtos.CreateTagPayload;
import com.neoflies.mystackoverflowapi.dtos.FindResult;
import com.neoflies.mystackoverflowapi.exceptions.BadRequestException;
import com.neoflies.mystackoverflowapi.exceptions.ResourceNotFoundException;
import com.neoflies.mystackoverflowapi.repositories.TagRepository;
import com.neoflies.mystackoverflowapi.repositories.UserRepository;
import com.neoflies.mystackoverflowapi.utils.AuthorizationUtils;
import com.neoflies.mystackoverflowapi.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.validation.Valid;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/tags")
public class TagController {
  @PersistenceContext
  EntityManager entityManager;

  @Autowired
  TagRepository tagRepository;

  @Autowired
  DateUtils dateUtils;
  
  @Autowired
  AuthorizationUtils authorizationUtils;
  
  @Autowired
  UserRepository userRepository;

  @GetMapping
  public ResponseEntity<FindResult<Tag>> findTags(
    @RequestParam(name = "search", required = false) String search,
    @RequestParam(name = "page", defaultValue = "1") Integer page,
    @RequestParam(name = "limit", defaultValue = "6") Integer limit,
    @RequestParam(name = "sortBy", defaultValue = "count") String sortBy,
    @RequestParam(name = "order", defaultValue = "desc") String order
  ) {
    String dataQuery = "SELECT * FROM tags";
    String countQuery = "SELECT count(*) FROM tags";
    if (search != null && !search.isBlank()) {
      String condition = String.format(" WHERE to_tsvector(name) @@ to_tsquery('%s')", search);
      dataQuery += condition;
      countQuery += condition;
    }
    dataQuery += String.format(" ORDER BY %s %s OFFSET %d LIMIT %d;", sortBy, order, (page - 1) * limit, limit);
    countQuery += ";";

    int total = ((Number) this.entityManager.createNativeQuery(countQuery).getSingleResult()).intValue();
    List<Tag> tags = this.entityManager.createNativeQuery(dataQuery, Tag.class).getResultList();
    Date startOfDay = this.dateUtils.getStartOfDay();
    Date endOfDay = this.dateUtils.getEndOfDay();
    Date startOfWeek = this.dateUtils.getStartOfWeek();
    Date endOfWeek = this.dateUtils.getEndOfWeek();
    String countByTimeQuery = "SELECT\n" +
      "\tcount(questions_tags.tags_id) AS count\n" +
      "FROM\n" +
      "\tquestions_tags\n" +
      "\tLEFT JOIN questions ON questions_tags.question_id = questions.id\n" +
      "WHERE\n" +
      "\tquestions_tags.tags_id = :tagId\n" +
      "\tAND questions.created_date >= :startDate\n" +
      "\tAND questions.created_date <= :endDate\n" +
      "GROUP BY\n" +
      "\tquestions_tags.tags_id;";
    for (Tag tag : tags) {
      Query dayQuery = this.entityManager.createNativeQuery(countByTimeQuery);
      dayQuery.setParameter("tagId", tag.getId());
      dayQuery.setParameter("startDate", startOfDay);
      dayQuery.setParameter("endDate", endOfDay);
      Integer day = ((Number) dayQuery.getResultList().stream().findFirst().orElse(0)).intValue();

      Query weekQuery = this.entityManager.createNativeQuery(countByTimeQuery);
      weekQuery.setParameter("tagId", tag.getId());
      weekQuery.setParameter("startDate", startOfWeek);
      weekQuery.setParameter("endDate", endOfWeek);
      Integer week = ((Number) weekQuery.getResultList().stream().findFirst().orElse(0)).intValue();

      tag.setDay(day);
      tag.setWeek(week);
    }

    FindResult<Tag> findResult = new FindResult<>(tags, total);
    return new ResponseEntity<>(findResult, HttpStatus.OK);
  }

  @GetMapping("/{name}")
  public ResponseEntity<Tag> findTagByName(@PathVariable String name) {
    Tag existedTag = this.tagRepository.findByName(name)
      .orElseThrow(() -> new ResourceNotFoundException("tags/tag-not-found", "Tag not found"));
    
    Optional<User> optionalUser = this.authorizationUtils.getCurrentUser();
    if (optionalUser.isPresent()) {
      User currentUser = optionalUser.get();
      Boolean watched = false;
      for (Tag tag : currentUser.getWatchTags()) {
        if (tag.getId().equals(existedTag.getId())) {
          watched = true;
          break;
        }
      }
      existedTag.setWatched(watched);
    }
    
    return new ResponseEntity<>(existedTag, HttpStatus.OK);
  }


  @PostMapping
  @PreAuthorize("hasAuthority('CREATE_TAG')")
  public ResponseEntity<Tag> createTag(@Valid @RequestBody CreateTagPayload body) {
    Boolean tagNameExists = this.tagRepository.existsByName(body.getName());
    if (tagNameExists) {
      throw new BadRequestException("tags/tag-name-exists", "Tag name already exists");
    }

    Tag tag = new Tag();
    tag.setId(UUID.randomUUID());
    tag.setName(body.getName());
    tag.setDescription(body.getDescription());

    Tag result = this.tagRepository.save(tag);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }
  
  @PatchMapping("/{id}/watch")
  @PreAuthorize("hasAuthority('WATCH_TAG')")
  public ResponseEntity<Tag> watchTag(@PathVariable String id) {
    Tag existedTag = this.tagRepository.findById(UUID.fromString(id))
      .orElseThrow(() -> new ResourceNotFoundException("watch-tag/tag-not-found", "Tag not found"));
    User currentUser = this.authorizationUtils.getCurrentUser().get();
    List<Tag> currentWatchedTags = currentUser.getWatchTags();
    for (Tag tag : currentWatchedTags) {
      if (tag.getId().toString() == id) {
        throw new BadRequestException("watch-tag/already-watch-this-tag", "You already watch this tag");
      }
    }
    
    currentWatchedTags.add(existedTag);
    currentUser.setWatchTags(currentWatchedTags);
    this.userRepository.save(currentUser);
    return new ResponseEntity<>(existedTag, HttpStatus.OK);
  }
  
  @PatchMapping("/{id}/unwatch")
  @PreAuthorize("hasAuthority('WATCH_TAG')")
  public ResponseEntity<Tag> unwatchTag(@PathVariable String id) {
    Tag existedTag = this.tagRepository.findById(UUID.fromString(id))
      .orElseThrow(() -> new ResourceNotFoundException("watch-tag/tag-not-found", "Tag not found"));
    User currentUser = this.authorizationUtils.getCurrentUser().get();
    List<Tag> currentWatchedTags = currentUser.getWatchTags();
    currentWatchedTags.remove(existedTag);
    currentUser.setWatchTags(currentWatchedTags);
    this.userRepository.save(currentUser);
    return new ResponseEntity<>(existedTag, HttpStatus.OK);
  }
}
